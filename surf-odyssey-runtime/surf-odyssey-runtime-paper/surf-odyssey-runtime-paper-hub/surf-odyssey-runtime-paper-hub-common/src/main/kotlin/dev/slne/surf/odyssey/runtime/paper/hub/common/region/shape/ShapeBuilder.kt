package dev.slne.surf.odyssey.runtime.paper.hub.common.region.shape

/** Marks the region and shape DSLs so their scopes do not leak into each other. */
@DslMarker
annotation class RegionDsl

/** Collects the included and excluded parts of a shape. */
@RegionDsl
class ShapeBuilder {
    private val includes = mutableListOf<Shape>()
    private val excludes = mutableListOf<Shape>()

    /** Adds a prism from [minY] to [maxY] whose outline is given by `point(x, z)` calls. */
    fun prism(minY: Int, maxY: Int, outline: PrismBuilder.() -> Unit) {
        includes += PrismBuilder().apply(outline).build(minY, maxY)
    }

    /** Adds the cuboid between both corners. */
    fun cuboid(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) {
        includes += Cuboid.of(BlockPoint(x1, y1, z1), BlockPoint(x2, y2, z2))
    }

    /** Removes every part declared in [block] from this shape. */
    fun exclude(block: ShapeBuilder.() -> Unit) {
        val inner = ShapeBuilder().apply(block)
        require(inner.excludes.isEmpty()) { "Exclusions cannot be nested" }
        excludes += inner.includes
    }

    /** Builds the shape from the parts added so far. */
    fun build(): Shape = CompoundShape(includes.toList(), excludes.toList())
}

/** Collects the corners of a prism outline. */
@RegionDsl
class PrismBuilder {
    private val points = mutableListOf<Point2>()

    /** Adds the next corner of the outline. */
    fun point(x: Int, z: Int) {
        points += Point2(x, z)
    }

    /** Builds the prism from the collected corners. */
    fun build(minY: Int, maxY: Int) = Prism(points.toList(), minY, maxY)
}

/** Builds a shape from prisms and cuboids, minus any excluded parts. */
fun shape(block: ShapeBuilder.() -> Unit): Shape = ShapeBuilder().apply(block).build()
