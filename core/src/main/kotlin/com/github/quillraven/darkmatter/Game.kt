package com.github.quillraven.darkmatter

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.graphics.profiling.GLProfiler
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import com.github.quillraven.darkmatter.asset.BitmapFontAsset
import com.github.quillraven.darkmatter.asset.I18NBundleAsset
import com.github.quillraven.darkmatter.asset.MusicAsset
import com.github.quillraven.darkmatter.asset.ShaderProgramAsset
import com.github.quillraven.darkmatter.asset.TextureAsset
import com.github.quillraven.darkmatter.asset.TextureAtlasAsset
import com.github.quillraven.darkmatter.audio.DefaultAudioService
import com.github.quillraven.darkmatter.ecs.system.AnimationSystem
import com.github.quillraven.darkmatter.ecs.system.AttachSystem
import com.github.quillraven.darkmatter.ecs.system.CameraShakeSystem
import com.github.quillraven.darkmatter.ecs.system.DamageSystem
import com.github.quillraven.darkmatter.ecs.system.DebugSystem
import com.github.quillraven.darkmatter.ecs.system.MoveSystem
import com.github.quillraven.darkmatter.ecs.system.PlayerAnimationSystem
import com.github.quillraven.darkmatter.ecs.system.PlayerColorSystem
import com.github.quillraven.darkmatter.ecs.system.PlayerInputSystem
import com.github.quillraven.darkmatter.ecs.system.PowerUpSystem
import com.github.quillraven.darkmatter.ecs.system.RemoveSystem
import com.github.quillraven.darkmatter.ecs.system.RenderSystem
import com.github.quillraven.darkmatter.event.GameEventManager
import com.github.quillraven.darkmatter.screen.LoadingScreen
import com.github.quillraven.darkmatter.ui.createSkin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.assets.async.AssetStorage
import ktx.async.KtxAsync
import ktx.collections.gdxArrayOf
import ktx.log.debug
import ktx.log.logger

private val LOG = logger<Game>()
const val V_WIDTH_PIXELS = 135
const val V_HEIGHT_PIXELS = 240
const val V_WIDTH = 9
const val V_HEIGHT = 16
const val UNIT_SCALE = 1 / 8f
const val PREFERENCE_MUSIC_ENABLED_KEY = "musicEnabled"
const val PREFERENCE_HIGHSCORE_KEY = "highScore"

class Game : KtxGame<KtxScreen>() {
    val gameViewport = FitViewport(V_WIDTH.toFloat(), V_HEIGHT.toFloat())
    val stage: Stage by lazy {
        val result = Stage(FitViewport(V_WIDTH_PIXELS.toFloat(), V_HEIGHT_PIXELS.toFloat()))
        Gdx.input.inputProcessor = result
        result
    }
    val assets: AssetStorage by lazy {
        KtxAsync.initiate()
        AssetStorage()
    }
    val gameEventManager by lazy { GameEventManager() }
    val audioService by lazy { DefaultAudioService(assets) }
    val engine: Engine by lazy {
        PooledEngine().apply {
            val atlas = assets[TextureAtlasAsset.GRAPHICS.descriptor]

            addSystem(DebugSystem(gameEventManager, audioService))
            addSystem(PowerUpSystem(gameEventManager, audioService).apply {
                setProcessing(false)
            })
            addSystem(PlayerInputSystem(gameViewport))
            addSystem(MoveSystem(gameEventManager).apply {
                setProcessing(false)
            })
            addSystem(DamageSystem(gameEventManager, audioService))
            addSystem(
                PlayerAnimationSystem(
                    atlas.findRegion("ship_base"),
                    atlas.findRegion("ship_left"),
                    atlas.findRegion("ship_right")
                ).apply {
                    setProcessing(false)
                }
            )

            println("TEST")
            println("TextureAtlasAsset")
            println(TextureAtlasAsset.values())
            println("descriptort")
            println(TextureAtlasAsset.GRAPHICS.descriptor)
            println("assets")
            println(assets)
            println("TEST")


            println(AttachSystem())
            println(AnimationSystem(atlas))
            addSystem(CameraShakeSystem(gameViewport.camera, gameEventManager))
            println(PlayerColorSystem(gameEventManager))
            addSystem(
                RenderSystem(
                    stage,
                    assets[ShaderProgramAsset.OUTLINE.descriptor],
                    gameViewport,
                    gameEventManager,
                    assets[TextureAsset.BACKGROUND.descriptor]
                )
            )
            println(RemoveSystem(gameEventManager))
        }
    }
    val preferences: Preferences by lazy { Gdx.app.getPreferences("dark-matter") }
    private val profiler: GLProfiler by lazy { GLProfiler(Gdx.graphics) }

    override fun create() {
        println("GAME :: create()")
        var isAssertLoadingFinish = false

        Gdx.app.logLevel = Application.LOG_ERROR
        profiler.enable()

        // load skin and go to LoadingScreen for remaining asset loading
        var old = System.currentTimeMillis()
        val assetRefs = gdxArrayOf(
            TextureAtlasAsset.values().filter { it.isSkinAtlas }.map { assets.loadAsync(it.descriptor) },
            BitmapFontAsset.values().map { assets.loadAsync(it.descriptor) },
            I18NBundleAsset.values().map { assets.loadAsync(it.descriptor) }
        ).flatten()
        KtxAsync.launch {
            println("coroutines start")
            assetRefs.joinAll()
            println("coroutines join end")
            // skin assets loaded -> create skin
            LOG.debug { "It took ${(System.currentTimeMillis() - old) * 0.001f} seconds to load skin assets" }
            old = System.currentTimeMillis()
            createSkin(assets)
            LOG.debug { "It took ${(System.currentTimeMillis() - old) * 0.001f} seconds to create the skin" }
            isAssertLoadingFinish = true
            // go to LoadingScreen to load remaining assets
            addScreen(LoadingScreen(this@Game))
            setScreen<LoadingScreen>()
        }


//        println("is load finish: $isAssertLoadingFinish")
//        println("assets.progress.isFinished: ${assets.progress.isFinished}")
//        println("assets.progress.isFailed: ${assets.progress.isFailed}")
////        println("assets.progress.percent: ${assets.progress.percent}")
//        println("assets.progress.total: ${assets.progress.total}")
//        while (!assets.progress.isFinished) {
////            println("in while")
////            println("assets.progress.percent: ${assets.progress.percent}")
//            if (isAssertLoadingFinish) break
//        }

//        while(true);
    }

    override fun render() {
        profiler.reset()
        super.render()
    }

    override fun dispose() {
        LOG.debug { "Dispose game with ${this.screens.size} screen(s)" }
        LOG.debug { "Last number of draw calls: ${profiler.drawCalls}" }
        LOG.debug { "Last number of texture bindings: ${profiler.textureBindings}" }
        MusicAsset.values().forEach {
            LOG.debug { "Reference count for music $it is ${assets.getReferenceCount(it.descriptor)}" }
        }

        super.dispose()
        assets.dispose()
        stage.dispose()
    }
}
