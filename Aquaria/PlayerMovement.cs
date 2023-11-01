using System;
using System.Collections;
using UnityEngine;
using UnityEngine.UI;

// The meat (note: serialized field values should be tuned in the Unity editor)
public class PlayerMovement : MonoBehaviour
{
    // How the sprite is rendered (component automatically created)
    private SpriteRenderer spriteR;
    // Gives physics (add component in Unity editor)
    private Rigidbody2D rb;
    // Gives animation (add component in Unity editor)
    private Animator anim;
    // Allows for 2D collisions (add component in Unity editor)
    private BoxCollider2D coll;
    // Allows for control over position and rotation (component automatically created)
    private Transform tr;
    // Integer ID for animator states
    private int animatorStateId;
    

    // If double jumping is available
    private bool doubleJump;
    // If the player is swimming
    public bool isSwimming;
    // If the player is crouching
    private bool isCrouching;
    // If the player can dash on land
    private bool canLDash = true;
    // If the player is currently dashing on land
    private bool isLDashing;
    // If the player is currently sprinting
    private bool isSprinting;
    // If the player can regenerate stamina
    private bool canRegenerate;
    // If the player can dash in water
    private bool canWDash = true;
    // If the player is currentlt dashing in water
    private bool isWDashing;

    [Header("Game Object Fields")]
    // All ground that can be jumped from
    [SerializeField] private LayerMask jumpableGround;
    // The stamina bar
    [SerializeField] private Slider staminaBar;
    // The flashlight
    [SerializeField] private GameObject flashlight;

    // Direction facing
    private float directionX;
    // Rate of change of the direction facing
    private float directionDX;
    // Current stamina
    private float currentStamina;
    // Regular movement speed
    private float moveSpeed2;
    // Regular swimming movement speed 
    private float swimSpeed2;

    // Parallel process that runs when regenerating stamina
    private Coroutine staminaRegen;
    // Timer for 0.02 seconds
    private WaitForSeconds regenTick = new(0.02f);

    [Header("Variables")]
    // Stamina drained per tick
    [SerializeField] private float staminaDrain = 20f;
    // Stamina gained per tick
    [SerializeField] private float staminaRegenRate = 2f;
    // Sprint speed factor
    [SerializeField] private float sprintMultiplier = 3f;
    // Current move speed (set to initial speed)
    [SerializeField] private float moveSpeed = 7f;
    // Proportionate to jump height
    [SerializeField] private float jumpForce = 14f;
    // Swim speed
    [SerializeField] private float swimSpeed = 5f;
    // Rotation speed in water
    [SerializeField] private float rotationSpeed = 0.3f;
    // Water deceleration factor
    [SerializeField] private float waterSlow = 0.999f;
    // Water rotatoinal deceleration factor
    [SerializeField] private float waterRotateSlow = 0.8f;
    // Time spent dashing
    [SerializeField] private float dashingTime = 0.2f;
    // Proportional to dash speed
    [SerializeField] private float dashingPower = 4f;
    // Cooldown between dashes
    [SerializeField] private float dashingCooldown = 1f;
    // Proportional to double jump height
    [SerializeField] private float doubleJumpPower = 12f;
    // Maximum stamina storage
    [SerializeField] private float maxStamina = 100f;

    // Enum to identify the movement state of the player
    private enum MovementState { Idle, Running, Jumping, Falling }

    // The jump sound effect
    [SerializeField] private AudioSource jumpSoundEffect;

    // Initialization
    private void Start() {
        // Get attached Unity components
        rb = GetComponent<Rigidbody2D>();
        anim = GetComponent<Animator>();
        spriteR = GetComponent<SpriteRenderer>();
        coll = GetComponent<BoxCollider2D>();
        tr = GetComponent<Transform>();

        // initialize variables
        staminaBar.maxValue = maxStamina;
        staminaBar.value = maxStamina;
        moveSpeed2 = moveSpeed;
        currentStamina = maxStamina;
        swimSpeed2 = swimSpeed;
        animatorStateId = Animator.StringToHash("state");
    }

    // Periodicly updates the state
    private void Update() {
        // If a dialogue is playing or a coroutine is managing dashing, return so no movement changes are made
        if (DialogueManager.GetInstance().dialogueIsPlaying || isLDashing || isWDashing) return;
        
        // If the player is swimming
        if (isSwimming) {
            // Manage if the player should spring
            WaterSprint();
            // Manage general water movement
            WaterMove();
        // Otherwise (on land)
        } else {
            // Manage if the player should sprint
            Sprint();
            // Manage general land movement
            LandMove();
        }

        // If the left alt key is pressed (in preparation for dashing)
        if (Input.GetKeyDown(KeyCode.LeftAlt)) {
            // If the player can dash on land is is not in water
            if (canLDash && !isSwimming) {
                // Start the coroutine to dash on land
                StartCoroutine(LDash());
            // If the player can dash in water and is in water
            } else if (canWDash && isSwimming) {
                // Start the coroutine to dash in water
                StartCoroutine(WDash());
            }
        }

        // Manage if the player should crouch
        Crouching();
        // Update the animation
        UpdateAnimationState();
    }

    // Manages if the player should move on land
    private void LandMove() {
        // If the user is pressing any left/right controlls
        directionX = Input.GetAxisRaw("Horizontal");
        // Set the player's velocity to the user-chosen direction while maintaining the veritcal velocity (jump/fall)
        rb.velocity = new Vector2(directionX * moveSpeed, rb.velocity.y);

        // If the player is on the ground
        if (IsGrounded()) {
            // Make sure the player is upright
            tr.rotation = Quaternion.Euler(0f, 0f, 0f);
            // Allow the player to double jump
            doubleJump = true;
            // If the user is not pressing a button to jump, return
            if (!Input.GetButtonDown("Jump")) return;
            // Otherwise, set the vertical velocity such that the player jumps (and play a sound :) )
            jumpSoundEffect.Play();
            rb.velocity = new Vector2(rb.velocity.x, jumpForce);
        // If the player is not on the ground and the jump button is pressed (indicating an attempt at a double jump)
        } else if (Input.GetButtonDown("Jump")) {
            // If the player cannot double jump, return
            if (!doubleJump) return;
            // Otherwise, set the veritical velocity such that the player double jumps (and play a sound)
            jumpSoundEffect.Play();
            rb.velocity = new Vector2(rb.velocity.x, doubleJumpPower);
            // Set so that the player cannot double jump until it reaches the ground again
            doubleJump = false;
        }
    }

    // Manages if the player should move on water
    private void WaterMove() {
        // If the player is pressing any left/right controlls
        directionX = Input.GetAxisRaw("Horizontal");
        // If the player is pressing any up/down controlls
        var directionY = Input.GetAxisRaw("Vertical");
        // Store the old velocity
        var oldVelocity = rb.velocity;
        // Slow the player down based on the water deceleration factors
        var velocity = new Vector2(oldVelocity.x * waterSlow, oldVelocity.y * waterSlow);

        // If the player is trying to turn
        if (directionX != 0) {
            // Turn the player
            tr.Rotate(0, 0, -directionX * rotationSpeed);
            // Update the direction speed (so that the turn can be slowed down without changing directionX)
            directionDX = directionX;
        } else {
            // If the player is not trying to turn, slow to a stop
            directionDX *= waterRotateSlow;
            // Rotate by the slowing value
            tr.Rotate(0, 0, -directionDX * rotationSpeed);
        }

        // If the player is trying to move forwards/backwards
        if (directionY != 0) {
            // Calculate the angle
            var angle = (tr.eulerAngles.z + 90) * Math.PI / 180f;
            // Set the x component of the velocity
            velocity.x = (float) Math.Cos(angle) * swimSpeed * directionY;
            // Set the y component of the velocity
            velocity.y = (float) Math.Sin(angle) * swimSpeed * directionY;
        }

        // Set the new velocity
        rb.velocity = velocity;
    }

    // Updates the animation of the player
    private void UpdateAnimationState() {
        // The movement state of the player
        MovementState state;

        // Depending on the direction, set the movement state and the flipX
        switch (directionX) {
            case > 0f:
                state = MovementState.Running;
                spriteR.flipX = false;
                break;
            case < 0f:
                state = MovementState.Running;
                spriteR.flipX = true;
                break;
            default:
                state = MovementState.Idle;
                break;
        }

        // Depending on the vertical velocity override the movement state
        state = rb.velocity.y switch {
            > .1f => MovementState.Jumping,
            < -.1f => MovementState.Falling,
            _ => state
        };

        // Set the animation state
        anim.SetInteger(animatorStateId, (int) state);
    }

    // Checks if the player is touching ground below them
    private bool IsGrounded() {
        // The player's collider bounds
        var bounds = coll.bounds;
        // The player's collider size
        var size = bounds.size;
        // Prevents the player from hanging on the edge of a platform
        size.x -= 1;
        // Check if the player's collider is touching ground below it
        return Physics2D.BoxCast(bounds.center, size, 0f, Vector2.down, .1f, jumpableGround);
    }

    // Called when the player is maintaining contact with an object
    private void OnTriggerStay2D(Collider2D collision) {
        // If the player is touching ground and not water, return
        if (IsGrounded() || !collision.gameObject.CompareTag("Water")) return;
        // The player is swimming.
        isSwimming = true;
        // No more gravity
        rb.gravityScale = 0f;
    }

    // Called when the player leaves contact with an object
    private void OnTriggerExit2D(Collider2D collision) {
        // If it is not water, return
        if (!collision.gameObject.CompareTag("Water")) return;
        // The player is no longer swimming
        isSwimming = false;
        // Bring gravity back
        rb.gravityScale = 3.5f;
    }

    // Dashes the player (on land)
    private IEnumerator LDash() {
        // Make the player not able to dash
        canLDash = false;
        isLDashing = true;
        // Record the original gravity
        var originalGravity = rb.gravityScale;
        // Remove gravity
        rb.gravityScale = 0f;
        // Make the player dash
        rb.velocity = new Vector2(directionX * moveSpeed * dashingPower, 0f);
        
        // Wait the duration of the dash
        yield return new WaitForSeconds(dashingTime);
        
        // Reinstate gravity
        rb.gravityScale = originalGravity;
        // No longer dashing
        isLDashing = false;
        
        // Wait until the cooldown can end
        yield return new WaitForSeconds(dashingCooldown);
        
        // The player can dash again
        canLDash = true;
    }

    // Dashes the player (on water)
    private IEnumerator WDash() {
        // Record the old velocity
        var oldVelocity = rb.velocity;
        // Make the player not able to dash
        canWDash = false;
        isWDashing = true;
        // Make the player dash (using the angle of the old velocity)
        rb.velocity = new Vector2(oldVelocity.x * waterSlow * dashingPower, oldVelocity.y * waterSlow * dashingPower);

        // Wait the duration of the dash
        yield return new WaitForSeconds(dashingTime);
        
        // No longer dashing
        isWDashing = false;

        // Wait until the cooldown can end
        yield return new WaitForSeconds(dashingCooldown);

        // The player can dash again
        canWDash = true;
    }

    // Checks if the player should crouch
    private void Crouching() {
        // If the crouch button is pressed and the player is not swimming
        if (Input.GetKeyDown(KeyCode.LeftControl) && !isSwimming) {
            // The player is crouching
            isCrouching = true;
            var trans = tr.transform;
            var position = trans.position;

            // Adjust the player position
            trans.position = new Vector3(position.x, position.y - 0.5f);
        }
        
        // If the crouch button is released, stop crouching
        if (Input.GetKeyUp(KeyCode.LeftControl)) {
            isCrouching = false;
        }

        // If crouching on land
        if (isCrouching && !isSwimming) {
            // Adjust player position and settings
            tr.transform.localScale = new Vector3(1f, 0.5f);
            jumpForce = 10f;
            doubleJumpPower = 8f;
        // If crouching on water
        } else {
            // Adjust player position and settings
            tr.transform.localScale = new Vector3(1f, 1f);
            jumpForce = 14f;
            doubleJumpPower = 12f;
        }
    }

    // Manages if the player should sprint in water
    private void WaterSprint() {
        // If the sprint button is pressed, the player is swimming and crouching, and if the stamina is above 0
        if (Input.GetKeyDown(KeyCode.LeftShift) && isSwimming && !isCrouching && currentStamina > 0 ) {
            // Sprint
            isSprinting = true;
            canRegenerate = false;
            // Stop stamina regen
            if (staminaRegen != null) {
                StopCoroutine(staminaRegen);
                staminaRegen = null;
            }
        }
        
        // If the sprint button is released
        if (Input.GetKeyUp(KeyCode.LeftShift)) {
            // Stop sprinting
            isSprinting = false;
            // Resume stamina regen
            canRegenerate = true;
            // Reset swim speed
            swimSpeed = swimSpeed2;
        }

        // If the player should sprint
        if (isSprinting) {
            // If the player has the stamina for it
            if (currentStamina > 0) {
                // Increase the swim speed (technically not a multiplier but whatever)
                swimSpeed = swimSpeed2 + sprintMultiplier;
                // Start draining stamina
                currentStamina -= staminaDrain * Time.deltaTime;
                // Stom regenerating stamina
                if (staminaRegen != null) {
                    StopCoroutine(staminaRegen);
                }
            } else {
                // Reset swim speed
                swimSpeed = swimSpeed2;
                // Resume stamina regeneration
                canRegenerate = true;
                isSprinting = false;
            }
        } else {
            // If the player can regenerate stamina but isn't
            if (canRegenerate && staminaRegen == null) {
                // Start regenerating stamina again
                staminaRegen = StartCoroutine(StaminaRegeneration());
            }
        }

        // Display the current stamina
        staminaBar.value = currentStamina;
    }

    // Manage if the player should spring on land
    private void Sprint() {
        // If the player is pressing the sprint button, isn't swimming, isn't crounching, and has the stamina
        if (Input.GetKeyDown(KeyCode.LeftShift) && !isSwimming && !isCrouching && currentStamina > 0) {
            // Sprint
            isSprinting = true;
            // Stop stamina regen
            canRegenerate = false; 
            if (staminaRegen != null) {
                StopCoroutine(staminaRegen);
                staminaRegen = null;
            }
        }
        
        // If the player releases the spring button
        if (Input.GetKeyUp(KeyCode.LeftShift)) {
            // Stop sprinting
            isSprinting = false;
            // Resume stamina regeneration
            canRegenerate = true;
            moveSpeed = moveSpeed2;
        }

        // If the player should sprint
        if (isSprinting) {
            // If the player has the stamina
            if (currentStamina > 0) {
                // Increase the run speed (technically not a multiplier but whatever)
                moveSpeed = moveSpeed2 + sprintMultiplier;
                // Start draining the stamina
                currentStamina -= (staminaDrain * Time.deltaTime);
                // Stop regenerating the stamina
                if (staminaRegen != null) {   
                    StopCoroutine(staminaRegen);
                }
            } else {
                // Reset the move speed
                moveSpeed = moveSpeed2;
                // Allow stamina regeneration
                canRegenerate = true;
                isSprinting = false;
            }
        } else {
            // Start stamina regeneration
            if (canRegenerate && staminaRegen == null) {
                staminaRegen = StartCoroutine(StaminaRegeneration());
            }
        }

        // Update the stamina bar
        staminaBar.value = currentStamina;
    }
    
    // Regenerate the stamina
    private IEnumerator StaminaRegeneration() {
        // Wait one second
        yield return new WaitForSeconds(1);
        
        // Slowly regenerate stamina as long as it is within the max
        while (currentStamina < maxStamina) {
            currentStamina += staminaRegenRate;
            staminaBar.value = currentStamina;
            
            yield return regenTick;
        }
        
        // Cap the stamina
        if (currentStamina > maxStamina) {
            currentStamina = maxStamina;
        }
        // Essentially deletes and nullifies the entire coroutine
        staminaRegen = null;
    }
}
