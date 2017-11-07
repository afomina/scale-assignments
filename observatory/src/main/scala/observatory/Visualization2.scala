package observatory

import com.sksamuel.scrimage.{Image, Pixel}

/**
  * 5th milestone: value-added information visualization
  */
object Visualization2 {

  /**
    * @param point (x, y) coordinates of a point in the grid cell
    * @param d00 Top-left value
    * @param d01 Bottom-left value
    * @param d10 Top-right value
    * @param d11 Bottom-right value
    * @return A guess of the value at (x, y) based on the four known values, using bilinear interpolation
    *         See https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_Square
    */
  def bilinearInterpolation(
    point: CellPoint,
    d00: Temperature,
    d01: Temperature,
    d10: Temperature,
    d11: Temperature
  ): Temperature = {
    d00 * (1 - point.x) * (1 - point.y) + d10 * point.x * (1 - point.y) + d01 * (1 - point.x) * point.y + d11 * point.x * point.y
  }

  /**
    * @param grid Grid to visualize
    * @param colors Color scale to use
    * @param tile Tile coordinates to visualize
    * @return The image of the tile at (x, y, zoom) showing the grid using the given color scale
    */
  def visualizeGrid(
    grid: GridLocation => Temperature,
    colors: Iterable[(Temperature, Color)],
    tile: Tile
  ): Image = {
    val colorsList = colors.toList

    val pixels = new Array[Pixel](256 * 256)
    var  i = 0
    for (lat <- (-89 to 90).reverse) { //TODO 256x256?
      for (lon <- -180 until 180) {
        val tempAtLoc = grid(GridLocation(lat, lon))
        val color = colorsList.find(_._1 == tempAtLoc).getOrElse((tempAtLoc, Color(0, 0, 0)))._2

        pixels(i) = Pixel.apply(color.red, color.green, color.blue, 100)
        i = i + 1
      }
    }

    Image(256, 256, pixels)
  }

}
