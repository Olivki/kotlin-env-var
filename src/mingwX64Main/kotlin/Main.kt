import net.ormr.envvar.Environment

public fun main() {
    println(Environment["gamer"])
    println(Environment["steve"])
    println(Environment.toMap().getValue("GAMER"))
    Environment["steve_is_bald"] = "not true!!!"
    println(Environment["steve_is_bald"])
    Environment["steve_is_bald"] = "he sure is!!!"
    println(Environment["steve_is_bald"])
}