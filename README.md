# ReplaceShareBufferDialog

> An LSPosed module that brings the **Share** button back to the clipboard preview on OxygenOS 15.

[English](#english) · [Русский](#%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%B8%D0%B9)

---

## English

### The problem

When you copy text on OxygenOS 15, a small preview pops up in the bottom-left corner of the screen. In stock AOSP it offers a **Share** chip. On OxygenOS 15 the preview only offers **Send to device**, and sharing the copied text through the system share sheet is not possible from there.

### What this module does

It makes the **Share** chip visible again in the clipboard preview. Tapping it opens the standard Android share sheet with the copied content, exactly as in AOSP. Nothing else in SystemUI is changed.

| Before | After |
| --- | --- |
| `[ Send to device ]` | `[ Share ] [ Send to device ]` |

### How it works

The OPlus SystemUI already contains everything needed: the Share chip exists in the layout, it is labelled by `ClipboardOverlayControllerExImpl.initOverlayActionChips()`, and its click handler (`mOnShareTapped`) is set. What is missing is the call that makes the chip visible.

`ClipboardOverlayController` has two variants of `setExpandedView`, chosen in `setClipData` by a feature flag:

- `setExpandedView(Runnable)` sets the click handler and calls `mView.showShareChip()`.
- `setExpandedView()` (no arguments) sets the click handler but **never calls `showShareChip()`**. On the tested device this is the variant that runs.

The module hooks the no-argument `setExpandedView()` and, after it returns, calls `mView.showShareChip()` if the clip type is not `OTHER`. This is the same condition the other variant uses. If the other variant runs, it has already made the chip visible and the extra call is harmless.

### Requirements

- Android 15 based OxygenOS (see "Compatibility" below)
- Root
- LSPosed (or a compatible Xposed framework supporting legacy modules, API 82)
- Tested on Magisk 30.1 + Vactor 2.2 by JingMatrix

### Installation

1. Install the APK.
2. Open LSPosed, go to **Modules**, and enable **ReplaceShareBufferDialog**.
3. Set the scope to **System UI** (`com.android.systemui`) only.
4. Restart SystemUI or reboot:

   ```
   su -c killall com.android.systemui
   ```
5. Copy some text. The preview should now show **Share**.

### Compatibility

Verified on the author's device running OxygenOS 15. Other OxygenOS or ColorOS builds were **not** tested. The module relies on these names, which are specific to the OPlus SystemUI build:

- `com.android.systemui.clipboardoverlay.ClipboardOverlayController`
- method `setExpandedView()` (no arguments)
- fields `mClipboardModel` and `mView`
- method `ClipboardOverlayView.showShareChip()`

If a firmware update renames any of them, the hook will log an error and the chip will stay hidden. SystemUI will not crash, because the hook body is wrapped in `try/catch`.

### Troubleshooting

| Symptom | What to check |
| --- | --- |
| Nothing changed | The module is enabled, the scope is **System UI**, and SystemUI was restarted |
| LSPosed log: `ClassNotFoundException` for the module package | `assets/xposed_init` must contain the full class name, `udonetsm.android.replacesharebufferdialog.ShareChipHook`, not just the package |
| LSPosed log: `ShareChipHook: ...` | The exception text names the field or method that was not found; the firmware differs from the tested one |
| The chip appears but does nothing | Open an issue and attach the `onShareButtonTapped` method decompiled from your SystemUI |

Logs: LSPosed → **Logs** tab, filter by the module name or `ShareChipHook`.

### Building

Requirements: Android Studio, an Xposed API jar (`api-82.jar`).

1. Put `api-82.jar` into `app/libs/`.
2. In `app/build.gradle.kts` the dependency must be `compileOnly`, otherwise the API classes end up inside the APK and conflict with the framework:

   ```kotlin
   compileOnly(files("libs/api-82.jar"))
   ```
3. Build a **debug** APK, or disable minification. R8 can rename `ShareChipHook` and break loading.

Project layout:

```
app/src/main/
├── AndroidManifest.xml                     # xposedmodule / xposedminversion / xposedscope meta-data
├── assets/xposed_init                      # udonetsm.android.replacesharebufferdialog.ShareChipHook
├── java/udonetsm/android/replacesharebufferdialog/
│   └── ShareChipHook.kt                    # the whole hook
└── res/values/arrays.xml                   # xposed_scope -> com.android.systemui
```

### Finding the right hook point on another firmware

The method used here was found by reading the SystemUI of the device. To repeat that on another build (Termux with root works fine):

```
export JAVA_OPTS="-Xmx1500M"
APK=/system_ext/priv-app/SystemUI/SystemUI.apk
P=com.android.systemui.clipboardoverlay

jadx --no-res -j 2 --single-class $P.ClipboardOverlayController --single-class-output ~/Ctrl.java $APK
jadx --no-res -j 2 --single-class $P.ClipboardOverlayView       --single-class-output ~/View.java $APK

grep -n "showShareChip\|setExpandedView" ~/Ctrl.java
```

Look for a `setExpandedView` variant that sets `mOnShareTapped` but never calls `mView.showShareChip()`.

### FAQ

**Why did OPlus remove it?** Unknown. The click handler and the chip label are still present, so it may simply be an oversight when merging with AOSP, or a deliberate emphasis on "Send to device". The code does not say.

**Does it work for images?** The hook runs for every clip type except `OTHER`, so the chip is shown for text and images. Only text was verified by the author.

**Is it safe?** The module only calls an existing method that makes an existing view visible. It does not change permissions, intents, or data. The share action is the stock AOSP one (`IntentCreator.getShareIntent`).

### Credits

- LSPosed and the Xposed API
- AOSP `ClipboardOverlayController` / `ClipboardOverlayView` as the reference behaviour

### License

Add a license of your choice (for example MIT or Apache-2.0) as a `LICENSE` file.

---

## Русский

### Проблема

При копировании текста на OxygenOS 15 слева внизу появляется небольшая миниатюра. В оригинальном AOSP в ней есть чип **«Поделиться»**. В OxygenOS 15 там только **«Отправить на устройство»**, а поделиться скопированным текстом через системное меню оттуда нельзя.

### Что делает модуль

Возвращает видимость чипа **«Поделиться»** в миниатюре буфера обмена. Нажатие открывает стандартное системное меню «Поделиться» со скопированным содержимым, как в AOSP. Больше ничего в SystemUI не меняется.

### Как это работает

В SystemUI от OPlus уже есть всё нужное: чип лежит в layout, подписан в `ClipboardOverlayControllerExImpl.initOverlayActionChips()`, обработчик нажатия (`mOnShareTapped`) задан. Не хватает только вызова, который делает чип видимым.

В `ClipboardOverlayController` два варианта `setExpandedView`, выбор между ними делает флаг в `setClipData`:

- `setExpandedView(Runnable)` задаёт обработчик и вызывает `mView.showShareChip()`;
- `setExpandedView()` без аргументов задаёт обработчик, но **не вызывает `showShareChip()`**. На проверенном устройстве выполняется именно он.

Модуль хукает `setExpandedView()` без аргументов и после его завершения вызывает `mView.showShareChip()`, если тип клипа не `OTHER`. Это то же условие, что и во втором варианте. Если выполняется второй вариант, чип уже виден, и повторный вызов ничего не ломает.

### Требования

- OxygenOS на базе Android 15 (см. «Совместимость»)
- Root
- LSPosed (или совместимый Xposed-фреймворк с поддержкой legacy-модулей, API 82)

### Установка

1. Установите APK.
2. В LSPosed откройте **Модули** и включите **ReplaceShareBufferDialog**.
3. Выберите область действия только **Система UI** (`com.android.systemui`).
4. Перезапустите SystemUI или перезагрузите телефон:

   ```
   su -c killall com.android.systemui
   ```
5. Скопируйте текст. В миниатюре должна появиться кнопка **«Поделиться»**.

### Совместимость

Проверено на устройстве автора с OxygenOS 15. Другие сборки OxygenOS и ColorOS **не тестировались**. Модуль опирается на имена, специфичные для SystemUI от OPlus:

- `com.android.systemui.clipboardoverlay.ClipboardOverlayController`
- метод `setExpandedView()` без аргументов
- поля `mClipboardModel` и `mView`
- метод `ClipboardOverlayView.showShareChip()`

Если обновление прошивки переименует что-то из этого, хук запишет ошибку в лог, а чип останется скрытым. SystemUI не упадёт: тело хука обёрнуто в `try/catch`.

### Если не работает

| Симптом | Что проверить |
| --- | --- |
| Ничего не изменилось | Модуль включён, область **Система UI**, SystemUI перезапущен |
| В логе LSPosed `ClassNotFoundException` с именем пакета модуля | В `assets/xposed_init` должно быть полное имя класса `udonetsm.android.replacesharebufferdialog.ShareChipHook`, а не только пакет |
| В логе `ShareChipHook: ...` | В тексте исключения указано, какое поле или метод не найдены: прошивка отличается от проверенной |
| Чип появился, но нажатие ничего не делает | Создайте issue и приложите декомпилированный метод `onShareButtonTapped` из вашего SystemUI |

Логи: LSPosed → вкладка **Журналы**, фильтр по имени модуля или `ShareChipHook`.

### Сборка

Нужны Android Studio и jar с Xposed API (`api-82.jar`).

1. Положите `api-82.jar` в `app/libs/`.
2. В `app/build.gradle.kts` зависимость должна быть именно `compileOnly`, иначе классы API попадут в APK и будут конфликтовать с фреймворком:

   ```kotlin
   compileOnly(files("libs/api-82.jar"))
   ```
3. Собирайте **debug** или отключите минификацию: R8 может переименовать `ShareChipHook` и сломать загрузку.

### Как найти точку хука на другой прошивке

Команды из англоязычного раздела выше (раздел «Finding the right hook point on another firmware») подходят и здесь: нужно декомпилировать `ClipboardOverlayController` и найти вариант `setExpandedView`, который задаёт `mOnShareTapped`, но не вызывает `mView.showShareChip()`.

### Частые вопросы

**Зачем OPlus это убрала?** Неизвестно. Обработчик и подпись чипа на месте, поэтому это может быть недосмотр при слиянии с AOSP или сознательный акцент на «Отправить на устройство». Код ответа не даёт.

**Работает ли для картинок?** Хук срабатывает для всех типов клипа, кроме `OTHER`, так что чип должен показываться и для текста, и для изображений.

**Безопасно ли?** Модуль только вызывает существующий метод, который делает видимым существующий элемент. Права, интенты и данные он не меняет. Действие «Поделиться» штатное (`IntentCreator.getShareIntent`).

### Благодарности

- LSPosed и Xposed API
- AOSP `ClipboardOverlayController` / `ClipboardOverlayView` как эталон поведения
