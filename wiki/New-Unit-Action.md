Suppose you have to add a new unit action available from map buttons.

1. Add UnitActionType in "UnitAction.kt".
This is a enum.
Add something like:

`DoSomething("Do something", {ImageGetter.getImage(".../DoSomething")}, key: 'd'), UncivSound.DoSmth),`

Or you can use existing icons or sounds to start up.

2. In "MapUnit.kt" make a new function, which triggers the action.

`fun doSomething()`
`{`
`action = "Do something"`
`}`

3. Add the action into the actionList

Call new function from GetNormalActions or getAdditionalActions.
The last one is used if smth changed after your action and you need to alternate the interface.

`addDoSomethingActions(actionList, unit)`

4. Make addDoSomething function:

make private function inside UnitActions object:

`private fun addDoSomethingActions(....){`
`actionList += UnitAction(UnitActionType.DoSomething,`
`action = {unit.doSomething()}}`
`}`