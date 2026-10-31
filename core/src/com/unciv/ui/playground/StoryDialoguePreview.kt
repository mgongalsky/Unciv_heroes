package com.unciv.ui.playground

import com.unciv.pure.domain.story.StoryChoice
import com.unciv.pure.domain.story.StoryDialogueNode
import com.unciv.ui.story.StoryDialoguePopup
import com.unciv.ui.utils.BaseScreen

/** Детерминированная реплика для Playground и визуальной проверки производственного окна. */
fun openStoryDialoguePreview(screen: BaseScreen) {
    val choices = arrayListOf(
        StoryChoice("ask", "Кто охраняет старую переправу?"),
        StoryChoice("help", "Я помогу вашему отряду. Расскажи, где искать пропавшего разведчика."),
        StoryChoice("leave", "Сейчас я должен идти. Мы поговорим позже.")
    )
    val node = StoryDialogueNode(
        id = "crossing", speaker = "Проводник", portrait = "missing-story-portrait",
        text = "За холмами начинается старая дорога. Мы видели огни у переправы, но разведчик так и не вернулся. Прежде чем идти дальше, реши, готов ли ты помочь нашему отряду.",
        choices = choices
    )
    StoryDialoguePopup(screen, node, choices) { true }.open(force = true)
}
