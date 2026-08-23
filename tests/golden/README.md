# UI golden tests

These tests render deterministic UI scenes into a hidden desktop OpenGL framebuffer and compare every pixel with a committed platform -specific PNG .

-Run `./gradlew desktop:goldenTest` to compare screenshots.
-Run `./gradlew desktop:goldenUpdate` after intentionally changing UI to replace baselines.
-Review files under `tests/golden/results/<platform>/` when a comparison fails .
-Commit approved images from `tests/golden/baseline/<platform>/`.Golden images are platform -specific because font rasterization and OpenGL drivers can differ between operating systems . The runner is visually headless(
    the GLFW window is hidden
), but still needs an environment capable of creating an OpenGL context.
