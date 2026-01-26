# PlethorasExtendedTools (Enhanced Containers)

Plugin na Paper 1.21.x / Java 21: własne craftowanie 4×4 w GUI + edytor receptur w grze.

## Komenda

Główna komenda to **`/4x4`**.

- Alias: **`/ecraft`** (działa tak samo jak `/4x4`)
- Definicja jest w [src/main/resources/plugin.yml](src/main/resources/plugin.yml)

### Subkomendy

- **`/4x4`**
  - Otwiera GUI craftingu 4×4 dla gracza.
- **`/4x4 new`**
  - Otwiera edytor nowej receptury.
- **`/4x4 edit [strona]`**
  - Otwiera selektor receptur do edycji (z paginacją).
- **`/4x4 sign_item <nazwa...>`**
  - Zmienia nazwę (display name) itemu trzymanego w main-hand.
  - Obsługuje kody kolorów przez `&` (np. `&cRed &aPickaxe`), które są zamieniane na `§`.

## Permisje

Permisje są zadeklarowane w [src/main/resources/plugin.yml](src/main/resources/plugin.yml):

- **`plethoras.tools.use`**
  - Pozwala używać `/4x4` i ogólnie komendy.
  - `default: true`
- **`plethoras.tools.admin`**
  - Wymagane do `/4x4 new` i `/4x4 edit`.
  - `default: op`
- **`plethoras.tools.sign_item`**
  - Wymagane do `/4x4 sign_item`.
  - `default: false`
  - W kodzie jest dodatkowy fallback: OP może użyć nawet bez tej permisji.

## Jak to działa (w skrócie)

### Crafting GUI

- `/4x4` otwiera inventory o stałym rozmiarze **54 sloty (9×6)**.
- GUI używa tytułów budowanych przez `GUITitleFormatter` (w tym znaki z resource-packa).
- Listener pilnuje zachowania GUI (m.in. blokady slotów, transferów i aktualizacji output).
- Jest wsparcie dla receptur własnych oraz dla craftingu „vanilla” (w tym logika dopasowania / symulacji craftingu dla 3×3).

### Edytor receptur

- `/4x4 new` tworzy nowy `RecipeDraft` i otwiera GUI edytora.
- `/4x4 edit` otwiera selektor, gdzie wybierasz recepturę do edycji.
- Edytor ma zabezpieczenia (np. input jest ograniczony do ilości 1 i sloty edytora są blokowane tam, gdzie trzeba).

### Receptury i config

- Konfiguracja jest w [src/main/resources/config.yml](src/main/resources/config.yml).
- Lista receptur siedzi pod `crafting.recipes`.
- W praktyce receptury dodaje się z gry przez `/4x4 new` (komentarze w config.yml prowadzą krok po kroku).

## Build i testy

Projekt ma dwa równoległe światy buildów:

- **Maven** (testy jednostkowe):
  - `mvn test`
- **integration-tests** (osobny moduł Maven, MockBukkit):
  - `cd integration-tests && mvn verify`
- **Gradle** (build pluginu / reobf jar):
  - `./gradlew clean test reobfJar`

Jeżeli coś nie przechodzi, zwykle warto odpalić te trzy kroki w tej kolejności.
