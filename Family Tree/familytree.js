/**
 * @typedef {Object} UnlinkedPerson
 * @property {string} name - Display name
 * @property {string} id - Unique identifier
 * @property {string} partner - Partner's ID
 * @property {string[]} children - Array of children's IDs
 */

/**
 * @typedef {Object} LinkedPerson
 * @property {string} name - Display name
 * @property {string} id - Unique identifier
 * @property {LinkedPerson} partner - Partner
 * @property {LinkedPerson[]} children - Array of children
 */

/**
 * @typedef {Object} Person
 * @property {string} name - Display name
 * @property {string} id - Unique identifier
 * @property {Person} partner - Partner
 * @property {Person[]} children - Array of children
 * @property {HTMLElement} div - DOM div name box
 * @property {function():number} getWidth - Function to get the div's width
 * @property {function():number} getPartnerWidth - Function to get the partner's div's width
 * @property {function():number} getChildrenWidth - Function to get the children's divs' width
 * @property {function():void} show - Function to add the div to the DOM
 * @property {function(number, number):void} place - Function to set the (X, Y) of the person's subtree's top left corner
 * @property {function():void} draw - Function to draw the lines connecting the divs of the person's subtree
 */


// People //


/**
 * Creates an array of people objects with unique ids.
 * @param {string[]} jsons - Array of JSON strings
 * @returns {LinkedPerson[]}
 */
async function fromJSON(jsons) {
    /** @type {UnlinkedPerson[]} */
    const people = [];
    for (const json of jsons) {
        people.push(...JSON.parse(json));
    }

    // Merge duplicates
    const peopleSet = people.filter((person, index) => {
        // Check if a previous duplicate exists
        const pIndex = people.findIndex(p => p.id == person.id);
        if (pIndex == index) return true;

        // Update the previous duplicate with the new information
        const duplicate = people[pIndex];
        if (!duplicate.partner) {
            duplicate.partner = person.partner;
        }
        if (duplicate.children && person.children) {
            for (const child of person.children) {
                if (!duplicate.children.includes(child)) {
                    duplicate.children.push(child);
                }
            }
        } else if (!duplicate.children) {
            duplicate.children = person.children;
        }
    });

    // UnlinkedPerson[] -> LinkedPerson[]
    linkPeople(peopleSet);

    return peopleSet;
}

/**
 * Transforms an array of {@link UnlinkedPerson} objects into {@link LinkedPerson} objects.
 * @param {UnlinkedPerson[]} people
 */
function linkPeople(people) {
    for (const person of people) {
        if (person.partner) person.partner = findPerson(people, person.partner);

        const childrenIDs = person.children;
        person.children = [];
        if (childrenIDs) {
            for (const childID of childrenIDs) {
                person.children.push(findPerson(people, childID));
            }
        }
    }
}

/**
 * Finds a person object by ID. If no one is found, {@link warn}s the client,
 * creates a new {@link LinkedPerson}, and pushes them into {@link people}.
 * @param {UnlinkedPerson[]|LinkedPerson[]} people - Array of objects to search
 * @param {string} id - ID to search for
 * @returns {UnlinkedPerson|LinkedPerson}
 */
function findPerson(people, id) {
    const person = people.find(p => p.id == id);
    if (person !== undefined) return person;

    warn("Person not found with ID: " + id);
    const newPerson = {
        name: id,
        id: id,
        children: []
    };
    const partner = people.find(p => p.partner && (p.partner == id || p.partner.id == id));
    if (partner !== undefined) {
        newPerson.partner = partner.id;
    }
    people.push(newPerson);
    return newPerson
}

/**
 * Warns the client.
 * @param {string} text
 */
function warn(text) {
    console.log("WARNING:", text);
    alert("WARNING: " + text);
}


// HTML //


const svg = document.querySelector("svg");
const tree = document.querySelector("#tree")
// Set the (X, Y) coordinates of the first name block
const TREE_X = -window.innerWidth / 2;
const TREE_Y = 0;
const form = document.querySelector("form");

if (form) {
    // When the files are submitted...
    form.addEventListener("submit", function(submitEvent) {
        submitEvent.preventDefault();
    
        const files = document.querySelector("#files").files;
        const jsons = [];
    
        // Catch them as they are read
        function fileSink(fileReaderOnLoadEvent) {
            jsons.push(fileReaderOnLoadEvent.target.result);
    
            // If all of the files have been read...
            if (jsons.length == files.length) {
                start(jsons);
            }
        }
    
        // Read them
        for (const file of files) {
            const reader = new FileReader();
            reader.onload = fileSink;
            reader.readAsText(file);
        }
    });
}

/**
 * Begin building and displaying the tree.
 * @param {string[]} jsons - Array of JSON strings
 */
function start(jsons) {
    // Hide the file chooser and reveal the tree
    if (form) form.classList.add("hidden");
    tree.classList.remove("hidden");

    // Parse the JSON and then build and display the tree
    fromJSON(jsons).then(people => layOut(people[0], people));
}


// TREE //


const PERSON_SPACE = 20;
const GEN_SPACE = 55;
const divs = [];

/**
 * Lays out the name boxes and connecting lines.
 * @param {LinkedPerson} root - Person to draw the tree from
 * @param {LinkedPerson[]} people - Array of all people
 */
function layOut(root, people) {
    // Clean up DOM
    svg.innerHTML = "";
    while (divs.length != 0) {
        divs.pop().remove();
    }

    // If a parent is available, draw the tree from them instead to allow searching up
    root = people.find(p => p.children && p.children.find(c => c.id == root.id)) || root;

    for (const person of people) {
        setUpPerson(person, people);
    }

    root.show();
    root.place(TREE_X, TREE_Y);
    root.draw();
}

/**
 * Transforms an array of {@link LinkedPerson} objects into {@link Person} objects.
 * @param {LinkedPerson} person - Person to set up
 * @param {LinkedPerson[]} people - Array of all people
 */
function setUpPerson(person, people) {
    const div = document.createElement("div");
    div.classList.add("person");
    div.innerText = person.name;
    div.onclick = function(e) {
        e.preventDefault();
        layOut(person, people);
    }
    divs.push(div);

    person.div = div;
    person.getWidth = function() {
        if (person._width) return person._width;
        return person._width = Math.max(person.getPartnerWidth(), person.getChildrenWidth());
    };
    person.getPartnerWidth = function() {
        if (person.partner) {
            return person.div.offsetWidth + PERSON_SPACE + person.partner.div.offsetWidth;
        }
        return person.div.offsetWidth; 
    };
    person.getChildrenWidth = function() {
        if (person.children) {
            return person.children.reduce((sum, child) => sum + child.getWidth(), 0) + (person.children.length - 1) * PERSON_SPACE;
        }
        return 0;
    };
    person.show = function() {
        tree.appendChild(person.div);
        if (person.partner) {
            tree.appendChild(person.partner.div);
            for (const child of person.children) {
                child.show();
            }
        }
    };
    person.place = function(x, y) {
        const personLeft = x + (person.getWidth() - person.getPartnerWidth()) / 2;
        move(person.div, personLeft, y);
        if (person.partner) {
            move(person.partner.div, personLeft + person.div.offsetWidth + PERSON_SPACE, y);
            let left = person.getChildrenWidth() > person.getPartnerWidth() ? x : x + (person.getPartnerWidth() - person.getChildrenWidth()) / 2;
            for (const child of person.children) {
                child.place(left, y + GEN_SPACE);
                left += child.getWidth() + PERSON_SPACE;
            }
        }
    };
    person.draw = function() {
        if (person.partner) {
            drawPartnerLine(person.div, person.partner.div);
            if (person.children.length > 1) {
                drawPartnerToChildrenLine(person.div, person.partner.div);
                let child;
                for (child of person.children) {
                    drawChildLine(child.div);
                    child.draw();
                }
                drawConnectChildrenLine(person.children[0].div, child.div);
            } else if (person.children.length == 1) {
                drawOnlyChildLine(person.div, person.partner.div);
                person.children[0].draw();
            }
        }
    };
}

/**
 * Move the name box to the (X, Y) coordinate.
 * @param {HTMLElement} div - Div to move
 * @param {number} x
 * @param {number} y
 */
function move(div, x, y) {
    div.style.top = div.offsetTop + y + "px";
    div.style.left = div.offsetLeft + x + "px";
}

/**
 * Creates a black SVG line and adds it to the DOM.
 * @returns {SVGLineElement}
 */
function createLine() {
    const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
    line.setAttribute("stroke", "#397A98");
    line.setAttribute("stroke-width", 2);
    svg.appendChild(line);
    return line;
}

/**
 * Draw the horizontal line connecting partners.
 * @param {HTMLElement} div1 - Div of partner 1
 * @param {HTMLElement} div2 - Div of partner 2
 */
function drawPartnerLine(div1, div2) {
    const rect1 = div1.getBoundingClientRect();
    const rect2 = div2.getBoundingClientRect();
    const y = (rect1.top + rect1.bottom) / 2;
    const line = createLine();
    line.setAttribute("x1", rect1.right);
    line.setAttribute("y1", y);
    line.setAttribute("x2", rect2.left + 1);
    line.setAttribute("y2", y);
}

/**
 * Draw the vertical line connecting the partner line to the children line.
 * @param {HTMLElement} div1 - Div of parent 1
 * @param {HTMLElement} div2 - Div of parent 2
 */
function drawPartnerToChildrenLine(div1, div2) {
    const rect1 = div1.getBoundingClientRect();
    const rect2 = div2.getBoundingClientRect();
    const x = (rect1.right + rect2.left) / 2;
    const y = (rect1.top + rect1.bottom) / 2;
    const line = createLine();
    line.setAttribute("x1", x);
    line.setAttribute("y1", y);
    line.setAttribute("x2", x);
    line.setAttribute("y2", y + GEN_SPACE / 2);
}

/**
 * Draw the vertical line connecting a child to the children line.
 * @param {HTMLElement} div - Div of child
 */
function drawChildLine(div) {
    const rect = div.getBoundingClientRect();
    const x = (rect.left + rect.right) / 2;
    const line = createLine();
    line.setAttribute("x1", x);
    line.setAttribute("y1", rect.top);
    line.setAttribute("x2", x);
    line.setAttribute("y2", (rect.top + rect.bottom - GEN_SPACE) / 2);
}

/**
 * Draw the vertical line connecting an only child to the partner line.
 * @param {HTMLElement} div1 - Div of parent 1
 * @param {HTMLElement} div2 - Div of parent 2
 */
function drawOnlyChildLine(div1, div2) {
    const rect1 = div1.getBoundingClientRect();
    const rect2 = div2.getBoundingClientRect();
    const x = (rect1.right + rect2.left) / 2;
    const y = (rect1.top + rect1.bottom) / 2;
    const line = createLine();
    line.setAttribute("x1", x);
    line.setAttribute("y1", y);
    line.setAttribute("x2", x);
    line.setAttribute("y2", y + GEN_SPACE + (rect1.top - rect1.bottom) / 2);
}

/**
 * Draw the horizontal line connecting the children's lines.
 * @param {HTMLElement} div1 - Div of first child
 * @param {HTMLElement} div2 - Div of last child
 */
function drawConnectChildrenLine(div1, div2) {
    const rect1 = div1.getBoundingClientRect();
    const rect2 = div2.getBoundingClientRect();
    const y = (rect1.top + rect1.bottom - GEN_SPACE) / 2;
    const line = createLine();
    line.setAttribute("x1", (rect1.left + rect1.right) / 2 - 1);
    line.setAttribute("y1", y);
    line.setAttribute("x2", (rect2.left + rect2.right) / 2 + 1);
    line.setAttribute("y2", y);
}

tree.mouseOver = false;

tree.onmouseover = () => tree.mouseOver = true;
tree.onmouseout = () => tree.mouseOver = false;

var startX, startY;

tree.onmousedown = function (e) {
    if (tree.mouseOver == false) return;
    e.preventDefault();
    startX = e.clientX;
    startY = e.clientY;
    document.onmousemove = onMouseMove;
    document.onmouseup = onMouseUp;
}

function onMouseMove(e) {
    e.preventDefault();
    const x = e.clientX - startX;
    const y = e.clientY - startY;
    for (const div of divs) {
        div.style.top = div.offsetTop + y + "px";
        div.style.left = div.offsetLeft + x + "px";
    }
    for (const line of svg.children) {
        line.setAttribute("x1", parseFloat(line.getAttribute("x1")) + x);
        line.setAttribute("y1", parseFloat(line.getAttribute("y1")) + y);
        line.setAttribute("x2", parseFloat(line.getAttribute("x2")) + x);
        line.setAttribute("y2", parseFloat(line.getAttribute("y2")) + y);
    }
    startX = e.clientX;
    startY = e.clientY;
}

function onMouseUp() {
    document.onmouseup = null;
    document.onmousemove = null;
}

tree.ontouchstart = function(e) {
    e.preventDefault();
    startX = e.clientX;
    startY = e.clientY;
    document.ontouchmove = onTouchMove;
    document.ontouchend = onTouchEnd;
}

function onTouchMove(e) {
    if (e.targetTouches.length != 1) return;
    e.preventDefault();
    const coords = e.targetTouches[0];
    const x = coords.clientX - startX;
    const y = coords.clientY - startY;
    for (const div of divs) {
        div.style.top = div.offsetTop + y + "px";
        div.style.left = div.offsetLeft + x + "px";
    }
    for (const line of svg.children) {
        line.setAttribute("x1", parseFloat(line.getAttribute("x1")) + x);
        line.setAttribute("y1", parseFloat(line.getAttribute("y1")) + y);
        line.setAttribute("x2", parseFloat(line.getAttribute("x2")) + x);
        line.setAttribute("y2", parseFloat(line.getAttribute("y2")) + y);
    }
    startX = coords.clientX;
    startY = coords.clientY;
}

function onTouchEnd() {
    document.ontouchend = null;
    document.ontouchmove = null;
}
