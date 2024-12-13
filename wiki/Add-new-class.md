If you need a new class with data, which must be saved and serialized you can have a look at the class Visitable as an example.
Remember several important points:

1. Inherit IsPartOfGameInfoSerialization:

`class Visitable() :
    IsPartOfGameInfoSerialization`

2. Provide a no-argument constructor for serialization. It is fine to have a constructor with arguments, but all must have default values. Otherwise, the error will occur during deserialization, which takes place every save including autosave.

3. Put the keyword @Transient before variables, which you do not need to save.

4. Make a clone() function (it is also required for serialization):

`    fun clone(): Visitable {
        val toReturn = Visitable()
        toReturn.visitedHeroesIDs = visitedHeroesIDs
        toReturn.visitedCivsIDs = visitedCivsIDs
        toReturn.turnsToRefresh = turnsToRefresh
        return toReturn
    }
`

5. If you have "parent" object, make sure that it is @Transient:

`    @Transient
    lateinit var parentTile: TileInfo
`

You need to set all transients of your class from "parent" class. Search for setTransient member-function of "parent" class. You can make also setTransient in your class just to call it from "parent" class. Make sure, that "parent" set transient does not overwrite existing values.