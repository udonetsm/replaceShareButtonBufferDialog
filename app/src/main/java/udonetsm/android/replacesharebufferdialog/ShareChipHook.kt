package udonetsm.android.replacesharebufferdialog

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class ShareChipHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        XposedHelpers.findAndHookMethod(
            "com.android.systemui.clipboardoverlay.ClipboardOverlayController",
            lpparam.classLoader,
            "setExpandedView",
            object : XC_MethodHook() {
                override fun afterHookedMethod(p: MethodHookParam) {
                    try {
                        val model = XposedHelpers.getObjectField(p.thisObject, "mClipboardModel")
                        val type = XposedHelpers.callMethod(model, "getType").toString()
                        if (type == "OTHER") return
                        val view = XposedHelpers.getObjectField(p.thisObject, "mView")
                        XposedHelpers.callMethod(view, "showShareChip")
                    } catch (t: Throwable) {
                        XposedBridge.log("ShareChipHook: $t")
                    }
                }
            }
        )
    }
}