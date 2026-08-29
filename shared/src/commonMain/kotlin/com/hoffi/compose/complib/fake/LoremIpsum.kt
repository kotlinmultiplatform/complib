package com.hoffi.compose.complib.fake

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val LoremIpsumString: String = "Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquid ex ea commodi consequat. Quis aute iure reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint obcaecat cupiditat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
val CiceroTextString: String = "At vero eos et accusamus et iusto odio dignissimos ducimus, qui blanditiis praesentium voluptatum deleniti atque corrupti, quos dolores et quas molestias excepturi sint, obcaecati cupiditate non provident, similique sunt in culpa, qui officia deserunt mollitia animi, id est laborum et dolorum fuga."
val EzekielString: String = "Et dabo ultionem meam super Idumeam per manum populi mei Israhel et facient in Edom iuxta iram meam et furorem meum et scient vindictam meam dicit Dominus Deus. Haec dicit Dominus Deus pro eo quod fecerunt Palestini in vindictam et ulti se sunt toto animo interficientes et implentes inimicitias veteres. Propterea haec dicit Dominus Deus ecce ego extendam manum meam super Palestinos et interficiam interfectores et perdam reliquias maritimae regionis. Faciamque in eis ultiones magnas arguens in furore et scient quia ego Dominus cum dedero vindictam meam super eos."

@Composable
fun LoremIpsum(modifier: Modifier = Modifier.padding(5.dp), randomWord: String? = null, repeats: Int = 20) {
    var result = ""
    if (randomWord != null) {
        (1..repeats).forEach { i -> result += "${i.toString().padStart(2)}.\n${randomlyReplaceWorsdWith(10, LoremIpsumString, replacement = randomWord)}\n\n" }
    } else {
        (1..repeats).forEach { i -> result += "${i.toString().padStart(2)}.\n${LoremIpsumString}\n\n" }
    }
    Column {
        Text(result, modifier)
        Text("Finis")
    }
}

@Composable
fun Cicero(modifier: Modifier = Modifier.padding(5.dp), randomWord: String? = null, repeats: Int = 10) {
    var result = ""
    if (randomWord != null) {
        (1..repeats).forEach { i -> result += "${i.toString().padStart(2)}.\n${randomlyReplaceWorsdWith(10, CiceroTextString, replacement = randomWord)}\n\n" }
    } else {
        (1..repeats).forEach { i -> result += "${i.toString().padStart(2)}.\n${CiceroTextString}\n\n" }
    }
    Column {
        Text(result, modifier)
        Text("Finis")
    }
}


@Composable
fun Ezekiel(modifier: Modifier = Modifier.padding(5.dp), randomWord: String? = null, repeats: Int = 10) {
    var result = ""
    if (randomWord != null) {
        (1..repeats).forEach { i -> result += "${i}.\n${randomlyReplaceWorsdWith(10, EzekielString, replacement = randomWord)}\n\n" }
    } else {
        (1..repeats).forEach { i -> result += "${i.toString().padStart(2)}.\n${EzekielString}\n\n" }
    }
    Column {
        Text(result, modifier)
        Text("Finis")
    }
}


fun randomlyReplaceWorsdWith(times: Int = 1, sentence: String, replacement: String) : String {
    val words = sentence.split(" ").toMutableList()
    val maxWords = words.size
    repeat(times) {
        val r = (0..(maxWords - 1)).random()
        words[r] = replacement
    }
    return words.joinToString(" ")
}
