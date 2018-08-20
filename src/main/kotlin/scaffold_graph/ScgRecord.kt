package scaffold_graph

class ScgRecord(val from: ScgVertex, val to: ScgVertex, val score: Double, val weight: Double, val info: String) {
    override fun toString(): String = "$from $to $score $weight \"$info\""

    class ScgVertex(val name: String, val rc: Boolean) {
        override fun toString(): String = "($name ${if (rc) "+" else "-"})"
    }
}

fun ScoreGraph.Edge.toScgRecord(): ScgRecord = ScgRecord(this.from.toScgVertex(), this.to.toScgVertex(), this.score, 0.0, "")

private fun ScoreGraphVertex.toScgVertex() = ScgRecord.ScgVertex(this.contigName, this.rc)