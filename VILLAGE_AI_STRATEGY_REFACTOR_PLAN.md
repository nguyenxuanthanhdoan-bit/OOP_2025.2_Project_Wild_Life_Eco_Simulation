# Ke hoach sua Village AI va Strategy System

## Danh gia ban ke hoach ban dau

Huong chia thanh cac nhom nen tang, selector, vong doi strategy va don dep la hop ly.
Tuy nhien, can dieu chinh mot so diem de tranh thay hard-code cu bang hard-code moi:

- Khong gan cong viec theo gioi tinh. Gioi tinh chi phuc vu sprite va sinh san.
- Khong giu strategy bang `instanceof HarvestStrategy/BoardBoatStrategy` trong selector.
- Khong nhả reservation trong `shouldInterrupt()`, vi method nay nen chi kiem tra trang thai.
- Khong dung hai boolean `avoidsSettlement` va `canEnterSettlement`, vi chung co the tao
  cac trang thai mau thuan.
- Fisherman phai duoc tinh la dan thuong trong cac hanh vi ve nha, tron nha, an uong va
  co the sinh san neu luat game cho phep.
- Thu hoach va danh ca phai tao ra thuc pham va dua ve kho; neu khong, day chi la animation.
- `GoHomeStrategy` can dung pathfinding. Di thang ve nha co the lam Human ket vao structure,
  nen day la loi tinh dung chu khong chi la van de tham my.

## Muc tieu kien truc

Moi thanh phan chi giu mot trach nhiem:

- `AnimalProfile`: kha nang sinh hoc va hanh vi chung cua loai.
- `HumanRole`: kha nang nghe nghiep cua Human.
- `StrategySelector`: chi sap xep do uu tien va chon strategy.
- Strategy: thuc hien mot hanh vi, khong tu quyet dinh luat cua loai/nghe.
- Manager: tim, reserve va release muc tieu dung chung.
- `BiomeGenerator`: chi sinh entity theo cau hinh, khong quyet dinh AI.

## Giai doan 1 - Sua du lieu Human va spawn

### 1. Tach gioi tinh khoi nghe nghiep

**Van de**

`BiomeGenerator.spawnVillagePeople()` dang bien moi Human nam thanh Hunter hoac Fisherman.
Ket qua la khong co male Villager va sinh san dung hoat dong.

**Sua**

Dung cac bien cau hinh rieng:

```java
VILLAGERS_PER_VILLAGE
HUNTERS_PER_VILLAGE
FISHERMEN_PER_VILLAGE
```

Spawn thanh ba nhom rieng. Trong nhom Villager:

```java
maleCount = villagerCount / 2;
femaleCount = villagerCount - maleCount;
```

Neu `villagerCount >= 2`, phai dam bao co it nhat mot male va mot female.
Hunter va Fisherman co gioi tinh rieng, nhung nghe nghiep khong duoc suy ra tu gioi tinh.

**File**

- `GameConfig.java`
- `BiomeGenerator.java`

### 2. Dua capability nghe nghiep vao HumanRole

**Van de**

Neu them `canHarvest()` va `canFish()` bang cac phep so san role rai rac trong `Human`,
code van se dai khi them Farmer, Builder hoac Trader.

**Sua**

Cho `HumanRole` giu capability cua role:

```java
VILLAGER  (harvest=true,  fish=false, hunt=false, useHouse=true, reproduce=true)
HUNTER    (harvest=false, fish=false, hunt=true,  useHouse=true, reproduce=false)
FISHERMAN (harvest=false, fish=true,  hunt=false, useHouse=true, reproduce=true)
```

`Human` chi delegate:

```java
public boolean canHarvest()       { return role.canHarvest(); }
public boolean canFish()          { return role.canFish(); }
public boolean canUseHouse()      { return role.canUseHouse(); }
public boolean canReproduceRole() { return role.canReproduce(); }
```

Khong dung gioi tinh de chon cong viec.

**File**

- `HumanRole.java`
- `Human.java`

### 3. Khoi phuc threat, shelter va prey rule

**Van de**

- `VILLAGER_PROFILE` dang mat `canBeScared` va `canHide`.
- `Human.canBeHuntedBy()` luon tra ve `false`.
- `ScaredStrategy` chi cho `isVillager()` vao nha, nen Fisherman khong tron duoc.

**Sua**

- Profile dan thuong bat lai `canBeScared(true)` va `canHide(true)`.
- `Human.canBeHuntedBy()` chi quyet dinh Human role co duoc phep lam con moi hay khong.
  Khong can check lai loai predator, vi `Animal.canHunt()` da check `canHunt`, level,
  kich thuoc va dong loai.
- Thay `human.isVillager()` trong shelter bang `human.canUseHouse()`.
- Hunter co the khong so va khong bi san; Villager/Fisherman dung luat role.

**File**

- `Human.java`
- `HumanRole.java`
- `ScaredStrategy.java`
- `StrategySelector.java`

## Giai doan 2 - Vong doi strategy va uu tien

### 4. Them lifecycle toi thieu cho IStrategy

**Van de**

Strategy hien khong biet khi nao minh bi thay. Reservation cua luong cay, ghe thuyen
hoac target co the bi giu mai.

**Sua**

Them default method de khong phai sua tat ca strategy ngay:

```java
default void onEnter(LivingBeing owner, World world) {}
default void onExit(LivingBeing owner, World world) {}
default boolean isCommittedTask() { return false; }
default boolean isInNonInterruptiblePhase() { return false; }
```

`LivingBeing.setStrategy()` phai:

1. Khong lam gi neu strategy moi la cung instance.
2. Goi `currentStrategy.onExit(...)`.
3. Gan strategy moi.
4. Goi `newStrategy.onEnter(...)`.

`shouldInterrupt()` phai la phep kiem tra khong co side effect.

**File**

- `IStrategy.java`
- `LivingBeing.java`
- `HarvestStrategy.java`
- `BoardBoatStrategy.java`

### 5. Chuan hoa thu tu uu tien cua Human

**Thu tu**

0. Neu dang o pha khong the ngat vat ly, vi du thuyen dang o ngoai bien, giu strategy.
1. Khat nguy hiem.
2. Co nguy hiem va role/profile cho phep so hai.
3. Doi nguy hiem.
4. Giu committed task neu chua hoan thanh va khong co nhu cau cao hon.
5. Ban dem ve nha/ngu.
6. Doi hoac khat thong thuong.
7. Hunter mang thit/het dan thi ve lang.
8. Sinh san.
9. Cong viec theo capability cua role.
10. Passive.

Selector khong check ten class cong viec. No chi dung:

```java
current.isCommittedTask()
current.shouldInterrupt(animal, world)
human.canHarvest()
human.canFish()
human.canHuntForVillage()
```

Fisherman cung phai vao `GoHomeStrategy` ban dem neu khong dang o ngoai bien.

**File**

- `StrategySelector.java`
- `Human.java`

## Giai doan 3 - Hoan thien chu trinh cong viec

### 6. HarvestStrategy va CropManager

**Van de**

- GardenBed bi reserve trong constructor.
- Khi bi ngat, reservation co the khong duoc nha.
- Thu hoach chi reset sprite, khong tao ra thuc pham.

**Sua**

- `CropManager.reserveNearestMatureCrop(human)` thuc hien tim va reserve atomically.
- `HarvestStrategy.onExit()` luon release neu chua thu hoach xong.
- `CropType` co `foodYield`.
- Khi thu hoach, them yield vao `Human.carriedFood`.
- Khi day tui hoac khong con cay can thu hoach, Human dung chinh strategy nay de dua
  hang ve `FoodStorage`, hoac dung mot helper logistics chung da co. Khong tao strategy
  moi neu chua can.

**File**

- `CropManager.java`
- `CropType.java`
- `GardenBed.java`
- `HarvestStrategy.java`
- `Human.java`

### 7. BoardBoatStrategy va CoastalManager

**Van de**

- Selector duyet toan bo entity de tim thuyen.
- Nhieu Fisherman co the cung chon mot ghe trong khi dang di toi.
- Human bi dich thang 100 px khi xuong thuyen.
- Ca danh duoc chi nam trong `carriedFood`, chua co chu trinh dua ve kho.

**Sua**

- `CoastalManager.reserveAvailableBoat(human)` va `releaseBoatReservation(...)`.
- Boat tinh ca ghe da reserve, khong chi passenger da len thuyen.
- Fisherman dung `PathNavigator` toi boarding point tren bo.
- Thuyen luu boarding/disembark point hop le.
- Khi xuong thuyen, tim diem bang `world.isValidPositionFor()`, khong sua position truc tiep.
- Sau khi cap ben, Fisherman mang ca ve `FoodStorage` roi moi ket thuc task.
- `BoardBoatStrategy.onExit()` release reservation neu chuyen di bi huy.

**File**

- `CoastalManager.java`
- `Boat.java`
- `BoardBoatStrategy.java`
- `StrategySelector.java`

### 8. GoHomeStrategy dung PathNavigator

**Van de**

Di thang bang steering co the ket vao nha, gieng, decorative hoac bo nuoc.

**Sua**

- Tim interaction point canh nha bang `PathNavigator.findInteractionPoint()`.
- Dung A* de di toi diem do.
- Chi goi `goSleep()` khi da vao interaction range.
- Neu navigator blocked, release target va chon nha khac.
- Fisherman, Villager va Hunter deu dung capability `canUseHouse()`.

**File**

- `GoHomeStrategy.java`
- `SettlementManager.java`

## Giai doan 4 - Settlement va garden traversal

### 9. Thay safe-zone hard block bang mot chinh sach duy nhat

**Van de**

Hai boolean `avoidsSettlement` va `canEnterSettlement` co the mau thuan. Ngoai ra,
`World.isValidPositionFor()` la collision rule, khong nen quyet dinh y dinh AI.

**Sua**

Dung mot enum policy trong `AnimalProfile`:

```java
SettlementPolicy.ALLOW
SettlementPolicy.AVOID
SettlementPolicy.BLOCK
```

- `ALLOW`: Human, Rabbit, Deer, Elephant.
- `AVOID`: Wolf, Tiger; A* tang cost khi di qua lang.
- `BLOCK`: chi dung cho loai tuyet doi khong duoc vao.

Mo rong navigation context:

```java
NORMAL
SEEKING_WATER
SEEKING_STRUCTURE
HUNTING
FLEEING
```

Voi `AVOID`, `NORMAL` nhan cost cao trong settlement, nhung `HUNTING` van co the di qua.
Khong dua current strategy vao `World.isValidPositionFor()`.

Buoc dau co the bo hard block va cho tat ca di qua, sau do them weighted cost. Khong nen
giu hard block cu trong khi cho doi he thong cost.

**File**

- `AnimalProfile.java`
- `PathNavigator.java`
- `TerrainNavigator.java`
- `World.java`
- `Wolf.java`
- `Tiger.java`

### 10. Toi uu garden threat

**Van de**

Moi Animal quet Human gan no, sau do lai quet toan bo GardenBed. Chi phi tang theo
so animal x so human x so garden.

**Sua**

- `CropManager.isGardenGuardedNear(position, radius)` la noi duy nhat thuc hien truy van.
- Cache trang thai guarded theo garden trong mot khoang ngan.
- Chi profile co `avoidsGuardedGarden=true` moi can kiem tra.
- `Animal` khong duyet danh sach GardenBed truc tiep.

**File**

- `CropManager.java`
- `AnimalProfile.java`
- `Animal.java`
- `StrategySelector.java`

## Giai doan 5 - Spawn va don dep

### 11. Khong force spawn chong len structure

**Van de**

`strictness = 0` va fallback bo qua khoang cach co the sinh Human/structure chong len nhau.
Tang attempts len 1000 chi che giau viec thuat toan khong tim duoc diem hop le.

**Sua**

- Khong bao gio bo qua `world.isValidPositionFor()` va collision clearance.
- Neu spawn thieu, ghi nhan so luong thieu va dung, khong force spawn.
- Neu can dam bao du dan, tao danh sach candidate walkable mot lan trong polygon, shuffle,
  roi lay cac diem dat clearance.
- Khong tu tao lang fallback neu map da quy dinh so lang co chu y, tru khi co config
  `ENABLE_FALLBACK_VILLAGES`.

**File**

- `BiomeGenerator.java`
- `GameConfig.java`

### 12. Xoa code va artifact khong dung

- Xoa `HumanGoal.java` neu khong chon goal system.
- Xoa `ThreatDetector.java`; `Animal.detectDangerousThreats()` da dung SpatialGrid va
  `isThreatenedBy()`, nen khong can nhanh rieng cho Human.
- Xoa `isFishing` neu khong con duoc su dung.
- Khong commit `game_output.log`, `output.log`, `sources_all.txt`.
- Bo cac `System.out.println()` spawn/debug.
- Sua trailing whitespace.

## Thu tu trien khai de giam rui ro

1. Sua spawn va role capability.
2. Khoi phuc threat/shelter/prey rule.
3. Them strategy lifecycle.
4. Sap xep lai StrategySelector.
5. Hoan thien Harvest task va dua nong san ve kho.
6. Hoan thien Fishing task, reservation va dua ca ve kho.
7. Chuyen GoHome sang A*.
8. Sua settlement traversal policy.
9. Toi uu garden threat.
10. Sua force spawn va don dep artifact.

Moi buoc phai compile duoc truoc khi sang buoc tiep theo.

## Kich ban kiem tra thu cong

Khong tao class test moi neu chua duoc yeu cau.

- Moi lang co male Villager, female Villager, Hunter va Fisherman dung cau hinh.
- Male/Female Villager co the tim nhau va sinh con.
- Villager/Fisherman gap Wolf/Tiger thi chay vao House; Hunter khong chay.
- Fisherman ve nha ban dem neu dang tren bo.
- Fisherman da ra bien phai hoan thanh chuyen di va cap ben an toan.
- Ca thu duoc va nong san thu hoach deu vao FoodStorage.
- Luong cay va ghe thuyen duoc release khi task bi huy.
- Human khong spawn trong structure hoac tren water.
- Wolf/Tiger tranh lang khi di thuong nhung co the vao khi dang san.
- Rabbit/Deer/Elephant khong bi hard block vo ly tai ranh gioi lang.
- Khong con log spawn lap lai trong console.
- Build cuoi:

```bash
javac -d /private/tmp/oop_compile $(rg --files src | rg '\.java$')
```

## Quyet dinh de xuat

- Cho Wolf/Tiger `SettlementPolicy.AVOID`, khong `BLOCK`.
- Khi dang san va con moi chay vao lang, su dung navigation context `HUNTING` de cho phep
  predator xam nhap. Villager van co co hoi tron vao House.
- Khong hoan GoHome A* den cuoi, vi steering hien tai co the gay ket thuc su.
- Chua can them strategy moi. Nang cap Harvest, BoardBoat, GoHome va cac manager hien co.
