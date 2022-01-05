package com.github.quillraven.darkmatter.ecs.component

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.utils.Pool
import ktx.ashley.mapperFor

/**
 * Property that says: this Entity can use any graphic's view. Example:
 * 1 view: show ship texture move LEFT
 * 2 view: show ship texture move FORWARD
 * 3 view: show ship texture move RIGHT
 */
class FacingComponent : Component, Pool.Poolable {
    var direction = FacingDirection.DEFAULT

    override fun reset() {
        direction = FacingDirection.DEFAULT
    }

    companion object {
        val mapper = mapperFor<FacingComponent>()
    }
}

enum class FacingDirection {
    LEFT, DEFAULT, RIGHT
}
