package dev.slne.surf.odyssey.runtime.paper.common

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import dev.slne.surf.odyssey.core.client.OdysseyClientInstance
import dev.slne.surf.odyssey.runtime.paper.common.menu.registerViews
import dev.slne.surf.odyssey.runtime.paper.common.skill.SkillRuntime
import dev.slne.surf.odyssey.runtime.paper.common.trade.TradeManager

abstract class OdysseyPaperMain : SuspendingJavaPlugin() {
    init {
        INSTANCE = this
    }

    abstract val serverType: OdysseyPaperServerType

    final override suspend fun onLoadAsync() {
        registerViews()

        OdysseyClientInstance.INSTANCE.onLoad()

        onLoaded()
    }

    final override suspend fun onEnableAsync() {
        OdysseyClientInstance.INSTANCE.onEnable()
        OdysseyPaperCommandManager.registerAllCommands()
        OdysseyPaperListenerManager.registerAllListeners()
        SkillRuntime.start()
        onEnabled()
    }

    final override suspend fun onDisableAsync() {
        onDisabled()
        TradeManager.shutdown()
        SkillRuntime.stop()
        OdysseyClientInstance.INSTANCE.onDisable()
    }

    open suspend fun onLoaded() {}
    open suspend fun onEnabled() {}
    open suspend fun onDisabled() {}

    final override fun onLoad() {
        super.onLoad()
    }

    final override fun onEnable() {
        super.onEnable()
    }

    final override fun onDisable() {
        super.onDisable()
    }

    companion object {
        lateinit var INSTANCE: OdysseyPaperMain
            private set
    }
}

internal val plugin get() = OdysseyPaperMain.INSTANCE