package observatory

import java.nio.file.Paths
import java.time.LocalDate

import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.functions._

/**
  * 1st milestone: data extraction
  */
object Extraction {

  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("Observatory")
      .config("spark.master", "local")
      .getOrCreate()

  /**
    * @param year             Year number
    * @param stationsFile     Path of the stations resource file to use (e.g. "/stations.csv")
    * @param temperaturesFile Path of the temperatures resource file to use (e.g. "/1975.csv")
    * @return A sequence containing triplets (date, location, temperature)
    */
  def locateTemperatures(year: Year, stationsFile: String, temperaturesFile: String)
  : Iterable[(LocalDate, Location, Temperature)] = {
    val tempDf = read(temperaturesFile, List("STN", "WBAN", "Month", "Day", "Temp"))
    val stationsDf = read(stationsFile, List("STN", "WBAN", "Lat", "Long"))

    val df = tempDf.join(stationsDf, tempDf("STN") === stationsDf("STN")
      && tempDf("WBAN") === stationsDf("WBAN"))

    def celsius(temp: Double): Double = (temp - 32) / 1.8

    df.map {
      case Row(stn: Double, wban: Double, month: Int, day: Int, temp: Double, lat: Double, long: Double) =>
        (LocalDate.of(year, month, day), Location(lat, long), celsius(temp))
    }.collect()
  }

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Temperature)]): Iterable[(Location, Temperature)] = {
    spark.sqlContext.createDataFrame(
      spark.sparkContext.parallelize(records.toList)
    ).groupBy("Location").agg(avg("Temperature")).map(row => (row.getAs(1), row.getAs(2))).collect()
  }

  def read(resource: String, headerColumns: List[String]): DataFrame = {
    val rdd = spark.sparkContext.textFile(fsPath(resource))

    // Compute the schema based on the first line of the CSV file
    val schema = dfSchema(headerColumns)

    val data =
      rdd
        .mapPartitionsWithIndex((i, it) => if (i == 0) it.drop(1) else it) // skip the header line
        .map(_.split(",").to[List])
        .map(row)

      spark.createDataFrame(data, schema)
  }

  def fsPath(resource: String): String =
    Paths.get(getClass.getResource(resource).toURI).toString

  def dfSchema(columnNames: List[String]): StructType =
    StructType(StructField(columnNames.head, StringType, nullable = false) ::
      columnNames.tail.map(name => StructField(name, DoubleType, nullable = false)))

  def row(line: List[String]): Row =
    Row.fromSeq(line) //Row(line.head.toString :: line.tail.map(_.toDouble): _*)
}
