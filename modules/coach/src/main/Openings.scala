package lila.coach

import chess.Color
import lila.analyse.Analysis
import lila.game.Pov

case class Openings(
    white: Openings.OpeningsMap,
    black: Openings.OpeningsMap) {

  def apply(c: Color) = c.fold(white, black)

  def merge(o: Openings) = Openings(
    white = white merge o.white,
    black = black merge o.black)
}

object Openings {

  val MAP_SIZE = 15

  case class OpeningsMap(m: Map[String, Results]) {
    def best: List[(String, Results)] = m.toList.sortBy(-_._2.nbGames) take MAP_SIZE
    def trim = OpeningsMap(m = best.toMap)
    def merge(o: OpeningsMap) = OpeningsMap {
      m.map {
        case (k, v) => k -> o.m.get(k).fold(v)(v.merge)
      } ++ o.m.filterKeys(k => !m.contains(k))
    }
    lazy val results = m.foldLeft(Results.empty) {
      case (res, (_, r)) => res merge r
    }
  }
  val emptyOpeningsMap = OpeningsMap(Map.empty)

  val empty = Openings(emptyOpeningsMap, emptyOpeningsMap)

  case class Computation(
      white: Map[String, Results.Computation],
      black: Map[String, Results.Computation]) {

    def aggregate(p: RichPov) =
      Ecopening.fromGame(p.pov.game).map(_.eco).fold(this) { eco =>
        copy(
          white = if (p.pov.color.white) agg(white, eco, p) else white,
          black = if (p.pov.color.black) agg(black, eco, p) else black)
      }

    private def agg(ops: Map[String, Results.Computation], eco: String, p: RichPov) =
      ops + (eco -> ops.get(eco).|(Results.emptyComputation).aggregate(p))

    def run = Openings(
      white = OpeningsMap(white.mapValues(_.run)).trim,
      black = OpeningsMap(black.mapValues(_.run)).trim)
  }
  val emptyComputation = Computation(Map.empty, Map.empty)
}
