package org.spbelect.blacklist.bot

import org.jooq.impl.DSL.field
import org.spbelect.blacklist.*

fun searchUik(uikId: Int): Map<Int, List<SearchResult>> {
  return prepareSearchQuery(SearchQuery(uik = uikId)) { q ->
    val year2persons = sortedMapOf<Int, MutableList<SearchResult>>()
    q.orderBy(field("year").desc(), field("status").asc()).distinct().forEach {it: SearchRecord ->
      val persons = year2persons.computeIfAbsent(it.year()) { mutableListOf() }
      persons.add(SearchResult(
          name = it.name(),
          status = mutableListOf(UikMemberStatus(
              uik = uikId, year = it.year(), uik_status = StatusEnum.values()[it.status()].label))
      ))
    }
    year2persons.toSortedMap(comparator = Comparator { year1, year2 -> year2 - year1 })
  }
}

fun handleUikBlacklist(uikId: Int, reply: (text: String) -> Unit) {
  val year2persons = searchUik(uikId)
  if (year2persons.isEmpty()) {
    reply("В реестре нет сведений о нарушениях в ${formatUikLabel(uikId)}")
  } else {
    val resultBuilder = StringBuilder("Нарушения в ${formatUikLabel(uikId)}\n")
    year2persons.forEach { year, persons ->
      resultBuilder.appendln("*$year*")
      resultBuilder.appendln(persons.map { "${it.status[0].uik_status} ${it.name}" }.joinToString(separator = ", "))
    }
    reply(resultBuilder.toString())
  }
}

fun formatUikLabel(uikId: Int): String {
  return "УИК $uikId"
}
