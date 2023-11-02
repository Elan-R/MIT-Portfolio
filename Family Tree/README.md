[Click to go to the website! (Just upload at least one JSON and click "Build Tree")](https://elan.ronen.life/projects/familytree/familytree.html)
<br>
<br>
A family tree builder that accepts JSON (see the examples for the format).  When an individual is selected, their parents' bloodline and spouses are shown (to fit the tree in two dimensions).
<br>
<br>
When making this project, I had to teach myself HTML, CSS, and more JavaScript than I had known before.  This took time and effort, as none of them are structured as nicely as I'd like.  I also ran into a lot of issues with my algorithm for placing the family tree's names.  I wanted the spacing between each box to be the same, which was easy for siblings and spouses, but hard between subtrees.  I also wanted parents of a subtree to be centered over their entire subtree, which proved difficult.  Through many sketches and much planning, I figured out one that worked beuatifully and recusively.  I also wanted to be able to take multiple JSON files and be able to merge them if they show parts of the same family tree.  This process took a lot of trial and error, but I figured it out.
