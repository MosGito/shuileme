package com.sleepshift.admin

import android.app.admin.DeviceAdminReceiver

/**
 * Device Owner / Device Admin 接收器。
 * 授予 Device Owner 后，系统会通过它派发设备策略回调。
 * 当前仅作为 Device Owner 的入口组件（后续步骤加入时区切换逻辑）。
 */
class DeviceAdminReceiver : DeviceAdminReceiver()
