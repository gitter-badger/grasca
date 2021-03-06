package com.github.pcejrowski.grasca

import java.time.Instant

import com.github.pcejrowski.grasca.model.render.{RenderResult, RenderedValues}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.collection.immutable.ListMap
import scalaj.http._

/**
  * These API endpoints are useful for retrieving raw data.
  *
  * @author pcejrowski
  * @param host - Graphite server address
  * @param port - Graphite server port
  */
class RenderAPI(private val host: String, private val port: Int = 80) {

  private lazy val renderEndpoint: String = s"http://$host:$port/render"

  private implicit val formats = DefaultFormats

  /**
    * Renders values from the target.
    *
    * @param target - specifies a path identifying one or several metrics, optionally with functions acting on those metrics.
    * @param from   - Timestamp from which to consider target. Default: 24 hours ago
    * @param until  - Timestamp until which to consider targe. Default: now
    * @return
    */
  def values(target: String, from: Instant = Instant.now.minus(DAY), until: Instant = Instant.now): Option[RenderedValues] = {
    val response: HttpResponse[String] = Http(s"$renderEndpoint")
      .param("target", target)
      .param("from", from.getEpochSecond.toString)
      .param("until", until.getEpochSecond.toString)
      .param("format", "json")
      .param("noNullPoints", "true")
      .asString

    parse(response.body)
      .extractOpt[List[RenderResult]]
      .map(_.map(entry => {
        val legend = entry.target
        val values: ListMap[Instant, Long] = ListMap(entry
          .datapoints
          .map {
            case List(value, epochTime) =>
              Instant.ofEpochSecond(epochTime) -> value
          }
          .sortBy(_._1)
          .reverse
          : _*)
        legend -> values
      }).sortBy(_._1)
      ).map(ListMap(_: _*))
  }
}

object RenderAPI {
  def apply(host: String, port: Int = 80) = new RenderAPI(host, port)
}