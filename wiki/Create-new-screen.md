1. Copy one of the existing screens into a new file.
For example:
I copied EmpireOverviewScreen into HeroOverviewScreen
That will give you automatically "Close" button.
Comment out everything, which makes errors. (In my case it was a "this" reference, which didn't work, because it was different class after copying) 

2. Change your screen class.
I added a reference to the actual hero in order to print his parameters.
`class HeroOverviewScreen(`
`...`
`private var viewingHero: MapUnit,`
`...`
`)`

3. In my case: 
add Table
add a page with that Table to a tabbedPager
select page
add to the stage

4. Call the screen by pushScreen function:

`UncivGame.Current.pushScreen(HeroOverviewScreen(unit.civInfo, unit))`