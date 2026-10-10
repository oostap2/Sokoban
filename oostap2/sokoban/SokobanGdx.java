package oostap2.sokoban;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class SokobanGdx extends ApplicationAdapter {

    private SpriteBatch batch;
    private TextureAtlas atlas;
    private Viewport gameViewport, uiViewport;
    private BitmapFont font;
    private GlyphLayout layout;
    private Vector2 touch;
    private byte[][] level;
    private float tileSize;
    private int x, y, levelIndex;
    private float uiSquareX, uiSquareY, buttonSize, textY, layoutWidth;
    private static final int[][] KEY = {{0,0},{0,1},{0,-1},{-1,0},{1,0}};
    private float nextLevelTimer;
    private boolean completed;

    private final TextureRegion[] texture = new TextureRegion[14];


    @Override
    public void create() {
        batch = new SpriteBatch();
        Gdx.gl.glClearColor(1, 1, 1, 1);

        atlas = new TextureAtlas(Gdx.files.internal("sokoban.atlas"));

        for (int i = 0; i < 14; i++) {
            texture[i] = atlas.findRegion(String.valueOf(i));
        }

        levelIndex = 1;
        level = new byte[9][9];
        loadLevel(levelIndex);

        gameViewport = new FitViewport(1, 1);
        uiViewport = new ScreenViewport();

        tileSize = 0.96f / 9;

        Texture fontTexture = new Texture(Gdx.files.internal("font/font.png"));
        fontTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font = new BitmapFont(Gdx.files.internal("font/font.fnt"), new TextureRegion(fontTexture), false);
        layout = new GlyphLayout();

        touch = new Vector2();

        nextLevelTimer = 0;
        completed = false;
    }

    @Override
    public void resize(int w, int h) {
        int size = Math.min(w, h);

        if (h > w) {
            // Portrait
            int uiHeight = h - size;

            gameViewport.update(size, size, true);
            gameViewport.setScreenBounds(0, uiHeight, size, size);

            uiViewport.update(w, uiHeight, true);
            uiViewport.setScreenBounds(0, 0, w, uiHeight);
        } else {
            // Landscape
            int uiWidth = w - size;

            gameViewport.update(size, size, true);
            gameViewport.setScreenBounds(0, 0, size, size);

            uiViewport.update(uiWidth, h, true);
            uiViewport.setScreenBounds(size, 0, uiWidth, h);
        }

        if (w == h) return;

        resizeUi();
    }

    private void resizeUi() {
        float w = uiViewport.getWorldWidth();
        float h = uiViewport.getWorldHeight();

        font.getData().setScale(Math.min(w, h) * 0.015f);
        layout.setText(font, "");
        if (h > w) {
            float halfH = h * 0.5f;
            buttonSize = Math.min(w, halfH);
            uiSquareX = (w - buttonSize) * 0.5f;
            uiSquareY = (halfH - buttonSize) * 0.5f;
            textY = halfH + halfH * 0.5f;
            layoutWidth = w;
        } else {
            float halfW = w * 0.5f;
            buttonSize = Math.min(halfW, h);
            uiSquareX = halfW + (halfW - buttonSize) * 0.5f;
            uiSquareY = (h - buttonSize) * 0.5f;
            textY = h * 0.5f;
            layoutWidth = halfW;
        }
        textY += layout.height;
        buttonSize /= 3;
    }

    @Override
    public void render() {
        logic();
        draw();
    }

    private void logic() {
        if (!completed) {
            keyboardInput();
            buttonInput();
            return;
        }

        nextLevelTimer += Gdx.graphics.getDeltaTime();

        if (nextLevelTimer < 1) return;

        completed = false;
        nextLevelTimer = 0;
        nextLevel();
    }

    private void buttonInput() {
        if (!Gdx.input.justTouched()) return;

        touch.set(Gdx.input.getX(), Gdx.input.getY());
        uiViewport.unproject(touch);

        if (btnColl(uiSquareX + buttonSize, uiSquareY)) move(1);
        if (btnColl(uiSquareX + buttonSize, uiSquareY + buttonSize * 2)) move(2);
        if (btnColl(uiSquareX, uiSquareY + buttonSize)) move(3);
        if (btnColl(uiSquareX + buttonSize * 2, uiSquareY + buttonSize)) move(4);
        if (btnColl(uiSquareX + buttonSize * 2, uiSquareY + buttonSize * 2)) nextLevel();
        if (btnColl(uiSquareX, uiSquareY + buttonSize * 2)) previousLevel();
    }

    private boolean btnColl(float btnX, float btnY) {
        return squarePointColl(touch.x, touch.y, btnX, btnY, buttonSize);
    }

    private boolean squarePointColl(float pointX, float pointY, float squareX, float squareY, float squareSize) {
        return
            !(pointX < squareX ||
            pointY < squareY ||
            pointX > squareX + squareSize ||
            pointY > squareY + squareSize);
    }

    private void keyboardInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) move(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) move(2);
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) move(3);
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) move(4);
        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) previousLevel();
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) nextLevel();
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) loadLevel(levelIndex);
    }

    private void move(int i) {
        if (i == 0) return;

        int dx = KEY[i][0];
        int dy = KEY[i][1];

        int nextY = y + dy;
        int nextX = x + dx;

        int boxY = y + 2 * dy;
        int boxX = x + 2 * dx;

        switch (level[nextY][nextX]) {
            case 2:
            case 5:
                break;
            case 3:
            case 6:
                if (level[boxY][boxX] != 0 && level[boxY][boxX] != 1) {
                    break;
                }

                if (level[boxY][boxX] == 1) {
                    level[boxY][boxX] = 6;
                } else {
                    level[boxY][boxX] = 3;
                }

                if (level[nextY][nextX] == 6) {
                    level[nextY][nextX] = 1;
                } else {
                    level[nextY][nextX] = 0;
                }
            case 0:
            case 1:
                if (level[y][x] == 7) {
                    level[y][x] = 1;
                } else {
                    level[y][x] = 0;
                }

                if (level[nextY][nextX] == 1) {
                    level[nextY][nextX] = 7;
                } else {
                    level[nextY][nextX] = 4;
                }

                y = nextY;
                x = nextX;
                break;
        }

        winCheck();
    }

    private void winCheck() {
        for(int i=0;i<9;++i)
            for(int j=0;j<9;++j)
                if(level[j][i]==3)
                    return;

//        nextLevel();
        completed = true;
    }

    private void loadLevel(int index) {
        Levels.get(index, level);
        for(int i = 0; i < 9; i++)
            for(int j = 0; j < 9; j++)
                if(level[j][i] == 4 || level[j][i] == 7){
                    y = j;
                    x = i;
                }
    }

    private void nextLevel() {
        levelIndex++;
        if (levelIndex > 15) {
            levelIndex = 1;
        }
        loadLevel(levelIndex);
    }

    private void previousLevel() {
        levelIndex--;
        if (levelIndex <= 0) {
            levelIndex = 15;
        }
        loadLevel(levelIndex);
    }

    private void draw() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawGame();
        drawUi();
    }

    private void drawGame() {
        gameViewport.apply();
        batch.setProjectionMatrix(gameViewport.getCamera().combined);

        batch.begin();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                batch.draw(texture[level[j][i]], tileSize * i + 0.02f, tileSize * (8 - j) + 0.02f, tileSize, tileSize);
            }
        }
        batch.end();
    }

    private void drawUi() {
        uiViewport.apply();
        batch.setProjectionMatrix(uiViewport.getCamera().combined);

        batch.begin();

        batch.draw(texture[9], uiSquareX, uiSquareY + buttonSize, buttonSize, buttonSize); // left
        batch.draw(texture[8], uiSquareX + buttonSize, uiSquareY, buttonSize, buttonSize); // down
        batch.draw(texture[12], uiSquareX + buttonSize * 2, uiSquareY+buttonSize, buttonSize, buttonSize); // right
        batch.draw(texture[13], uiSquareX + buttonSize, uiSquareY + buttonSize*2, buttonSize, buttonSize); // up
        batch.draw(texture[11], uiSquareX, uiSquareY + buttonSize * 2, buttonSize, buttonSize); // previous
        batch.draw(texture[10], uiSquareX + buttonSize * 2, uiSquareY + buttonSize * 2, buttonSize, buttonSize); // next

        layout.setText(font, "" + levelIndex, Color.BLACK, layoutWidth, 1, false);
        font.draw(batch, layout, 0, textY);

        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        atlas.dispose();
        font.dispose();
    }
}
