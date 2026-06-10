# VILLAGE AI & STRATEGY SYSTEM — IMPLEMENTATION PLAN

> Tổng hợp từ `VILLAGE_AI_STRATEGY_REFACTOR_PLAN.md` sau khi đã đọc và xác nhận toàn bộ codebase.
> Mỗi bước phải compile thành công trước khi sang bước tiếp theo.

---

## Bước 1 — Sửa Spawn và Role Capability

### 1.1 Tách giới tính khỏi nghề nghiệp

**Vấn đề**: `BiomeGenerator.spawnVillagePeople()` (line 585) biến mọi nam thành Hunter/Fisherman, không có male Villager → sinh sản không hoạt động.

**Thay đổi**:

#### [MODIFY] `GameConfig.java`
```java
// Thêm:
public final int VILLAGERS_PER_VILLAGE  = 10; // Thay HUMANS_PER_VILLAGE
public final int FISHERMEN_PER_VILLAGE  = 4;
// Giữ nguyên:
public final int HUNTERS_PER_VILLAGE    = 2;
```
> `HUMANS_PER_VILLAGE` (14) hiện đang gộp cả villager + fisherman. Tách ra.
Trước khi xóa hằng cũ, dùng `rg HUMANS_PER_VILLAGE` và cập nhật toàn bộ nơi tham chiếu;
không giữ đồng thời hai nguồn cấu hình cho cùng một số lượng.

#### [MODIFY] `BiomeGenerator.spawnVillagePeople()`
```java
// Spawn 3 nhóm riêng biệt:
int villagerCount = config.VILLAGERS_PER_VILLAGE;
int maleCount   = villagerCount / 2;       // ≥1 nếu villagerCount >= 2
int femaleCount = villagerCount - maleCount;

// Nhóm 1: male Villager
spawnGroup(maleCount,   pos -> new Human(pos, MALE, VILLAGER, center, radius));
// Nhóm 2: female Villager
spawnGroup(femaleCount, pos -> new Human(pos, FEMALE, VILLAGER, center, radius));
// Nhóm 3: Hunter (giữ nguyên logic cũ)
spawnGroup(config.HUNTERS_PER_VILLAGE, pos -> new Hunter(pos, center, radius));
// Nhóm 4: Fisherman (variant tuỳ ý, role = FISHERMAN)
spawnGroup(config.FISHERMEN_PER_VILLAGE, pos -> new Human(pos, randomVariant(), FISHERMAN, center, radius));
```
Không dùng xác suất 50% hay `instanceof` để phân vai.

**Files**: `GameConfig.java`, `BiomeGenerator.java`

---

### 1.2 Đưa capability vào `HumanRole`

**Vấn đề**: Nếu thêm `canHarvest()` bằng phép so sánh role rải rác trong `Human`, code sẽ dài khi thêm Farmer/Builder.

**Thay đổi**:

#### [MODIFY] `HumanRole.java`
```java
public enum HumanRole {
    VILLAGER (true,  false, false, true, true,  true),
    HUNTER   (false, false, true,  true, false, false),
    FISHERMAN(false, true,  false, true, true,  true);

    private final boolean canHarvest;
    private final boolean canFish;
    private final boolean canHunt;
    private final boolean canUseHouse;
    private final boolean canReproduce;
    private final boolean canBeHunted;

    HumanRole(boolean harvest, boolean fish, boolean hunt, boolean house,
              boolean reproduce, boolean canBeHunted) { ... }

    public boolean canHarvest()   { return canHarvest; }
    public boolean canFish()      { return canFish; }
    public boolean canHunt()      { return canHunt; }
    public boolean canUseHouse()  { return canUseHouse; }
    public boolean canReproduce() { return canReproduce; }
    public boolean canBeHunted()  { return canBeHunted; }
}
```

#### [MODIFY] `Human.java`
```java
public boolean canHarvest()       { return role.canHarvest(); }
public boolean canFish()          { return role.canFish(); }
public boolean canHuntForVillage(){ return role.canHunt() && shouldHuntForVillage(); }
public boolean canUseHouse()      { return role.canUseHouse(); }
public boolean canReproduceRole() { return role.canReproduce(); }
```
Xóa `isVillager()` khỏi logic phân công và sinh sản. `canMateWith()` dùng
`canReproduceRole()`, giới tính và `homeSettlement`, nhờ vậy Villager/Fisherman có thể
tham gia sinh sản theo đúng capability mà không hard-code tên role.

Đổi tên `VILLAGER_PROFILE` thành `CIVILIAN_PROFILE` nếu profile này được dùng chung cho
Villager và Fisherman, để tên không gây hiểu nhầm.

**Files**: `HumanRole.java`, `Human.java`

---

### 1.3 Khôi phục Threat, Shelter và Prey Rule

**Vấn đề**:
- `VILLAGER_PROFILE` thiếu `canBeScared(true)` và `canHide(true)`.
- `Human.canBeHuntedBy()` luôn `return false`.
- `ScaredStrategy` chỉ cho phép `isVillager()` vào nhà.

**Thay đổi**:

#### [MODIFY] `Human.java`
```java
// VILLAGER_PROFILE:
AnimalProfile.builder()
    .canBeScared(true)
    .canHide(true)
    ...

// canBeHuntedBy():
@Override
public boolean canBeHuntedBy(Animal predator) {
    return role.canBeHunted(); // Không gắn quyền bị săn với quyền sinh sản
}
```

Đồng thời sửa:

```java
@Override
public boolean canReproduce() {
    return role.canReproduce() && super.canReproduce()
            && reproductionCooldown <= 0;
}

@Override
public boolean canMateWith(Animal other) {
    // Cả hai đều phải có quyền sinh sản, khác giới và thuộc cùng Settlement.
}
```

Cooldown sinh sản phải nằm trên `Animal/Human`, không nằm trong instance
`MatingStrategy`, vì strategy có thể bị tạo lại và làm mất cooldown.

Triển khai tối thiểu:

```java
// Animal
private float reproductionCooldown;

public void update(float deltaTime) {
    reproductionCooldown = Math.max(0, reproductionCooldown - deltaTime);
    ...
}

public void startReproductionCooldown() {
    reproductionCooldown = GameConfig.getInstance().REPRODUCTION_COOLDOWN_SECONDS;
}
```

Sau khi sinh con, `MatingStrategy` gọi cooldown cho cả hai cha mẹ và xóa
`cooldownTimer` cục bộ của chính strategy.

#### [MODIFY] `ScaredStrategy.java`
```java
// Thay: human.isVillager()
// Bằng: human.canUseHouse()
```

#### [MODIFY] `StrategySelector.java`
```java
// Thêm check canBeScared() trước khi chọn ScaredStrategy cho Human
if (human.getProfile().canBeScared() && animal.hasDangerousThreats()) {
    return currentOrNew(animal, ScaredStrategy.class, new ScaredStrategy());
}
```

**Files**: `Animal.java`, `Human.java`, `HumanRole.java`, `MatingStrategy.java`,
`ScaredStrategy.java`, `StrategySelector.java`

---

### 1.4 Gắn Human với một Settlement ổn định

**Vấn đề**: `spawnVillageStructures()` và `spawnVillagePeople()` đang gọi
`findVillageClusterCenter()` riêng. Vì method này có yếu tố random, `homeCenter` của Human
có thể khác tâm Settlement chứa nhà, giếng và kho của chính làng đó.

**Thay đổi**:

- `spawnVillageStructures()` trả về ánh xạ `Map<MapPolygonObject, Settlement>`.
- `spawnVillagePeople()` nhận ánh xạ này và dùng đúng Settlement đã tạo.
- `Settlement` đăng ký `House`, `Well`, `FoodStorage`, `GardenBed`.
- `Human` giữ tham chiếu `homeSettlement` thay vì chỉ dựa vào một tâm/bán kính gần đúng.
- Việc tìm nhà, kho, giếng và kiểm tra hai Human cùng làng đều đi qua Settlement.
- Human con sinh ra phải kế thừa đúng `homeSettlement` của cha mẹ.

Không cần tạo class Village mới; tận dụng `Settlement` hiện có.

**Files**: `Settlement.java`, `SettlementManager.java`, `BiomeGenerator.java`,
`Human.java`, `ForageStrategy.java`

---

## Bước 2 — Strategy Lifecycle

### 2.1 Thêm `onEnter` / `onExit` vào `IStrategy`

**Vấn đề**: Strategy bị thay mà không biết → reservation GardenBed, Boat bị giữ mãi.

**Thay đổi**:

#### [MODIFY] `IStrategy.java`
```java
default void onEnter(LivingBeing owner, World world) {}
default void onExit(LivingBeing owner, World world)  {}
default boolean isCommittedTask()           { return false; }
default boolean isInNonInterruptiblePhase() { return false; }
```

#### [MODIFY] `LivingBeing.setStrategy()`
```java
public void setStrategy(IStrategy newStrategy) {
    if (newStrategy == currentStrategy) return;
    if (currentStrategy != null) currentStrategy.onExit(this, world);
    currentStrategy = newStrategy;
    if (currentStrategy != null) currentStrategy.onEnter(this, world);
}
```

`onEnter/onExit` phải chấp nhận trường hợp `world == null`, vì constructor của Human
đang gán `PassiveStrategy` trước khi entity được thêm vào World.

#### [MODIFY] `HarvestStrategy.java`
```java
// Constructor nhận GardenBed đã được CropManager reserve cho đúng owner.
// onExit release reservation bằng owner nếu task chưa hoàn tất.
@Override
public void onEnter(LivingBeing owner, World world) {
    // Chỉ validate reservation, không reserve lần hai.
}
@Override
public void onExit(LivingBeing owner, World world) {
    if (targetBed != null && !harvested) {
        targetBed.releaseReservation(owner);
    }
}
```

#### [MODIFY] `BoardBoatStrategy.java`
```java
@Override
public void onExit(LivingBeing owner, World world) {
    if (!finished && targetBoat != null) {
        world.getCoastalManager().releaseBoatReservation(targetBoat, owner);
    }
}
```

Reservation phải gắn với owner, không dùng một boolean chung. Boat có sức chứa 2 người
nên cần reserve theo số ghế, ví dụ `Set<UUID> reservedPassengers`.

Nếu Human đã lên thuyền và thuyền đang ngoài biển, strategy không được tháo passenger
trong `onExit`; giai đoạn đó phải trả về `isInNonInterruptiblePhase() == true`.

**Files**: `IStrategy.java`, `LivingBeing.java`, `HarvestStrategy.java`, `BoardBoatStrategy.java`

---

### 2.2 Chuẩn hóa thứ tự ưu tiên trong `StrategySelector` (phần Human)

**Thứ tự mới**:
```
0. isInNonInterruptiblePhase()  → giữ nguyên
1. Khát nguy hiểm               → ForageStrategy
2. hasDangerousThreats() && canBeScared() → ScaredStrategy
3. Đói nguy hiểm                → Forage hoặc Hunter theo capability
4. isCommittedTask() && !shouldInterrupt() → giữ nguyên
5. Hunter mang đủ thịt/hết đạn  → HunterStrategy (về kho)
6. Ban đêm && canUseHouse()     → GoHomeStrategy
7. Đói / khát thông thường      → ForageStrategy
8. canReproduce() và hết cooldown → MatingStrategy
9. canHarvest() → HarvestStrategy
   canFish()    → BoardBoatStrategy (via CoastalManager)
   canHunt()    → HunterStrategy
10. PassiveStrategy
```

Không cần nhánh `hasDangerousThreats() → Passive` cho role không biết sợ. Hunter không
biết sợ thì tiếp tục hành vi hiện tại; không được đứng yên chỉ vì thấy một entity cấp cao.

Selector không dùng `instanceof` để phân vai/công việc, không check giới tính và không
duyệt `world.getEntities()`. Một điểm phân nhánh cấp cao `animal instanceof Human` vẫn
chấp nhận được để chuyển sang selector logic của Human; việc chọn nghề phải dựa trên capability.

**Files**: `StrategySelector.java`, `Human.java`

---

## Bước 3 — Hoàn thiện Chu trình Công việc

### 3.1 HarvestStrategy và CropManager

**Vấn đề**: Thu hoạch chỉ reset sprite, không tạo thực phẩm.

**Thay đổi**:

#### [MODIFY] `CropManager.java`
```java
// Thêm:
public GardenBed reserveNearestMatureCrop(Human human); // tìm + reserve atomically theo UUID
public void releaseCrop(GardenBed bed, Human human);
public boolean isGardenGuardedNear(Vector2 pos, float radius); // cache, thay thế duyệt thẳng
```

#### [MODIFY] `GardenBed.java` / `CropType.java`
- Thêm `foodYield` vào `CropType`.
- Thay `boolean beingHarvested` bằng owner reservation, ví dụ `UUID reservedBy`.
- Chỉ owner đang giữ reservation mới được harvest hoặc release.

#### [MODIFY] `HarvestStrategy.java`
- Khi thu hoạch xong: `human.addCarriedFood(cropYield)`.
- Khi đầy túi: đi về `FoodStorage` và deposit ngay trong cùng strategy, không tạo strategy mới.
- Tìm `FoodStorage` qua `human.getHomeSettlement()`, không quét toàn bộ World.
- Nếu kho đầy, kết thúc task an toàn và giữ sản phẩm trong `carriedFood`; không làm mất yield.

**Files**: `CropManager.java`, `CropType.java`, `GardenBed.java`, `HarvestStrategy.java`, `Human.java`

---

### 3.2 BoardBoatStrategy và CoastalManager

**Vấn đề**: Selector duyệt toàn bộ entity tìm thuyền; nhiều Fisherman chọn cùng thuyền; đẩy thẳng 100px khi xuống.

**Thay đổi**:

#### [MODIFY] `CoastalManager.java`
```java
public Boat reserveAvailableBoat(Human fisherman); // reserve một ghế
public void releaseBoatReservation(Boat boat, LivingBeing owner);
```

#### [MODIFY] `Boat.java`
- Lưu `boardingPoint` (trên bờ) và `disembarkPoint` (trên bờ, tính trước).
- Lưu reservation theo từng passenger; tổng passenger + reservation không vượt capacity.
- Không khởi hành nếu passenger/reservation đang ở trạng thái không nhất quán.

#### [MODIFY] `BoardBoatStrategy.java`
- Dùng `PathNavigator` đến `boardingPoint`.
- Khi xuống thuyền: `world.isValidPositionFor()` để tìm điểm an toàn, không `position.add()` trực tiếp.
- `onExit()` release reservation nếu chuyến bị hủy.
- Sau khi cập bến, nếu `human.hasCarriedFood()` → deposit vào `FoodStorage` gần nhất trong homeArea.
- Khi thuyền đang `SAILING_OUT`, `FISHING` hoặc `SAILING_BACK`, task là non-interruptible.
- Sau khi cập bến, Human xuống tại `disembarkPoint`, rồi dùng A* về kho; không teleport.

**Files**: `CoastalManager.java`, `Boat.java`, `BoardBoatStrategy.java`, `StrategySelector.java`

---

### 3.3 GoHomeStrategy dùng PathNavigator (A*)

**Vấn đề**: Đi thẳng bằng steering → kẹt vào House, giếng, decorative (lỗi tính đúng, không phải thẩm mỹ).

**Thay đổi**:

#### [MODIFY] `GoHomeStrategy.java`
- Thay vector steering bằng `PathNavigator.moveTo(human, world, interactionPoint, ...)`.
- `interactionPoint` = điểm đứng hợp lệ gần cửa nhà (tính qua `world.isValidPositionFor()`).
- Nếu navigator blocked → chọn nhà khác qua `SettlementManager`.
- Chỉ gọi `goSleep()` khi đã vào interaction range, không tự dịch chuyển Human vào tâm nhà.
- Áp dụng cho Villager, Hunter và Fisherman đang trên bờ (tất cả `canUseHouse()`).
- `Human.goSleep()` có thể đặt vị trí nội bộ vào House khi entity đã hidden, nhưng lúc thức
  dậy phải tìm được điểm hợp lệ cạnh nhà; không fallback ra đúng tâm solid House.

**Files**: `GoHomeStrategy.java`, `SettlementManager.java`

---

## Bước 4 — Settlement Traversal Policy

### 4.1 Thay safe-zone hard-block bằng `SettlementPolicy` enum

**Vấn đề**: `World.isAnimalBlockedFromSettlement()` hard-block mọi Animal trên cạn không phải Human → Thỏ/Hươu/Voi cũng bị chặn vô lý; Sói/Hổ không thể xâm nhập.

**Thay đổi**:

#### [MODIFY] `AnimalProfile.java`
```java
public enum SettlementPolicy { ALLOW, AVOID, BLOCK }
// Thêm field: private final SettlementPolicy settlementPolicy;
// Mặc định: ALLOW
```

#### Gán policy theo loài:
| Loài | Policy |
|------|--------|
| Human | ALLOW |
| Rabbit, Deer, Elephant | ALLOW |
| Wolf, Tiger | AVOID |

#### [MODIFY] `World.isAnimalBlockedFromSettlement()`
```java
SettlementPolicy policy = entity.getProfile().getSettlementPolicy();
if (policy == SettlementPolicy.BLOCK) return true;
return false; // ALLOW và AVOID vẫn là vị trí có thể đi qua
```

Sau khi refactor có thể đổi tên helper thành `isPhysicallyBlockedFromSettlement()` để
phản ánh đúng việc method này chỉ xử lý policy `BLOCK`.

`World.isValidPositionFor()` chỉ quyết định vị trí có hợp lệ về vật lý hay không. Nó không
được đọc `currentStrategy`, vì như vậy collision phụ thuộc vào AI state và A* có thể nhận
kết quả khác nhau giữa các frame.

`AVOID` được xử lý bằng traversal cost trong A*, không phải hard-block:

```java
NORMAL             -> cost cao khi đi qua Settlement
SEEKING_STRUCTURE  -> cost bình thường với Human
HUNTING            -> cost thấp hơn NORMAL để predator có thể truy đuổi vào làng
FLEEING             -> cost theo profile của entity đang chạy
```

`PathNavigator` phải truyền `MovementContext` xuống cả:

- `TerrainNavigator.findPath(...)`
- `TerrainNavigator.hasWalkableLine(...)`
- Hàm tính step cost của A*

Nếu chỉ thêm cost vào `findPath()` nhưng direct-line vẫn bỏ qua policy, entity vẫn có thể
đi thẳng xuyên làng mà không dùng A*.

**Files**: `AnimalProfile.java`, `Wolf.java`, `Tiger.java`, `World.java`,
`PathNavigator.java`, `TerrainNavigator.java`, `HunterStrategy.java`

---

### 4.2 Tối ưu Garden Threat

**Vấn đề**: Mỗi Animal quét Human rồi quét GardenBed → O(animal × human × garden).

**Thay đổi**:

#### [MODIFY] `CropManager.java`
```java
// Cache trạng thái guarded, refresh mỗi ~0.5s
public boolean isGardenGuardedNear(Vector2 pos, float radius);
```

#### [MODIFY] `AnimalProfile.java`
- Thêm `boolean avoidsGuardedGarden` (mặc định `false`).
- Gán capability theo luật gameplay, ví dụ Deer/Rabbit có thể tránh vườn đang được bảo vệ.
  Không hard-code `instanceof Deer` trong manager.

#### [MODIFY] `Animal.java`
- `hasGardenThreat()` chỉ gọi `cropManager.isGardenGuardedNear(...)` nếu `profile.avoidsGuardedGarden`.

`CropManager` nên cache trạng thái theo từng GardenBed hoặc cụm vườn. Không cache một
boolean toàn cục, vì một làng có người bảo vệ không có nghĩa mọi vườn trên map đều được bảo vệ.

**Files**: `CropManager.java`, `AnimalProfile.java`, `Animal.java`, `StrategySelector.java`

---

## Bước 5 — Spawn & Dọn dẹp

### 5.1 Không force spawn chồng lên structure

**Vấn đề**: `strictness = 0` + attempts 1000 che giấu thất bại spawn.

**Thay đổi**:

#### [MODIFY] `BiomeGenerator.java`
- Không bao giờ bỏ qua `world.isValidPositionFor()`.
- Nếu thiếu entity sau đủ attempts → trả về số lượng spawn thành công, không force.
- Chỉ in cảnh báo khi bật debug flag; không spam console trong game bình thường.
- Xây candidate list từ walkable tiles trong polygon, shuffle, lấy điểm đạt clearance.
- Mỗi candidate vẫn phải kiểm tra hitbox/collider đầy đủ; walkable tile center chưa đủ để
  đảm bảo một entity lớn không chạm water hoặc structure.
- Cập nhật SpatialGrid sau mỗi entity được thêm trước khi kiểm tra candidate tiếp theo,
  để các entity trong cùng batch không chồng lên nhau.

#### [MODIFY] `GameConfig.java`
- Thêm `ENABLE_FALLBACK_VILLAGES = false` nếu muốn kiểm soát.

**Files**: `BiomeGenerator.java`, `GameConfig.java`

---

### 5.2 Xóa code và artifact không dùng

| Item | Hành động |
|------|-----------|
| `HumanGoal.java` | **Xóa** — không được gọi ở đâu |
| `ThreatDetector.java` | **Xóa** — logic đã có trong `Animal.hasDangerousThreats()` với SpatialGrid |
| `Human.isFishing` field | **Xóa** nếu không còn dùng sau khi sửa BoardBoat |
| `System.out.println()` debug spawn | **Xóa** |
| `game_output.log`, `output.log`, `sources_all.txt` | **Không commit** — thêm vào `.gitignore` |

Nếu các artifact đã được Git theo dõi thì thêm `.gitignore` chưa đủ; cần xóa chúng khỏi
index/commit của branch.

**Files**: `HumanGoal.java` (delete), `ThreatDetector.java` (delete), `Human.java`,
`BiomeGenerator.java`, `.gitignore`

---

## Kịch bản Kiểm tra Thủ công

Sau khi hoàn thành từng bước:

- [ ] Mỗi làng có male Villager, female Villager, Hunter và Fisherman đúng cấu hình.
- [ ] Male + Female Villager có thể tìm nhau và sinh con.
- [ ] Cooldown sinh sản vẫn còn hiệu lực sau khi MatingStrategy bị ngắt rồi được chọn lại.
- [ ] Villager/Fisherman gặp Wolf/Tiger chạy vào House; Hunter không chạy.
- [ ] Fisherman về nhà ban đêm nếu đang trên bờ; nếu đang trên biển thì hoàn thành chuyến và cập bến an toàn.
- [ ] Cá thu được và nông sản thu hoạch đều vào `FoodStorage`.
- [ ] Luống cây và ghế thuyền được release khi task bị hủy.
- [ ] Hai Fisherman không reserve vượt quá sức chứa của một Boat.
- [ ] Một Human không thể release luống cây hoặc ghế thuyền của Human khác.
- [ ] Human không spawn trong structure hoặc trên water.
- [ ] Wolf/Tiger tránh làng khi di chuyển thường, nhưng có thể vào khi đang săn.
- [ ] Rabbit/Deer/Elephant không bị hard-block tại ranh giới làng.
- [ ] Không còn log spawn lặp lại trong console.
- [ ] Build cuối sạch: `javac -d /private/tmp/oop_compile $(rg --files src | rg '\.java$')`

---

## Quyết định Kiến trúc đã Chốt

| Vấn đề | Quyết định |
|--------|------------|
| Wolf/Tiger vào làng | `SettlementPolicy.AVOID`, cho phép xâm nhập khi context là `HUNTING` |
| GoHome A* | **Làm ngay** (Bước 3.3) — lỗi tính đúng, không phải thẩm mỹ |
| Strategy mới | Không thêm. Nâng cấp Harvest, BoardBoat, GoHome và Manager hiện có |
| HumanGoal | Xóa |
| ThreatDetector | Xóa, dùng `Animal.hasDangerousThreats()` |

---

## Điều kiện hoàn thành từng bước

Mỗi bước chỉ được coi là hoàn thành khi:

1. Compile toàn bộ source thành công.
2. `git diff --check` không báo lỗi format mới.
3. Không còn reservation mồ côi do strategy vừa sửa tạo ra.
4. Không thêm lookup `world.getEntities()` vào loop AI mỗi frame.
5. Không dùng giới tính để quyết định nghề nghiệp.
6. Không để `World.isValidPositionFor()` phụ thuộc vào strategy hiện tại.
