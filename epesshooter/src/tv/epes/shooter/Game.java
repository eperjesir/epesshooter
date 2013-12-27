package tv.epes.shooter;

import java.util.Iterator;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

public class Game implements ApplicationListener{

	private static final int CAMERA_WIDTH = 480;
	private static final int CAMERA_HEIGHT = 800;
	
	Sound levelUpSound;
	Sound hitSound;
	Texture tower;
	Rectangle rect_tower;
	Texture baddie;
	Rectangle rect_baddie;
	Texture bullet;
	Rectangle rect_bullet;
	Array<Bullet2D> bullets;
	Array<Rectangle> baddies;
	OrthographicCamera camera;
	SpriteBatch batch;
	BitmapFont font;
	BitmapFont gofont;
	long startTime;
	long lastShotTime;
	long lastBaddieTime;
	int score;
	int lives;
	int difficultyMultiplier;
	int levelUp;
	private boolean gameOver;
	
	@Override
	public void create() {
		camera = new OrthographicCamera();
		camera.setToOrtho(false, CAMERA_WIDTH, CAMERA_HEIGHT);
		batch = new SpriteBatch();
		font = new BitmapFont();
		font.scale(1f);
		font.setColor(Color.BLACK);
		gofont = new BitmapFont();
		gofont.scale(2f);
		gofont.setColor(Color.BLACK);
		bullets = new Array<Bullet2D>();
		baddies = new Array<Rectangle>();
		startTime = TimeUtils.millis();
		score = 0;
		lives = 5;
		gameOver = false;
		levelUp = 0;
		difficultyMultiplier = 1;
		
		/** Tower sprite **/
		tower = new Texture(Gdx.files.internal("tower.png"));
		rect_tower = new Rectangle();
		rect_tower.width = 64;
		rect_tower.height = 64;
		rect_tower.x = (CAMERA_WIDTH / 2) - (rect_tower.width / 2);
		rect_tower.y = (CAMERA_HEIGHT) - (rect_tower.height) - 20;
		
		/** Bullet sprite **/
		bullet = new Texture(Gdx.files.internal("bullet.png"));		
		
		/** Baddy sprite **/
		baddie = new Texture(Gdx.files.internal("baddie.png"));
		
		//Sound
		hitSound = Gdx.audio.newSound(Gdx.files.internal("shoot.ogg"));
		levelUpSound = Gdx.audio.newSound(Gdx.files.internal("levelUp.ogg"));
		
	}

	@Override
	public void dispose() {
		tower.dispose();
		bullet.dispose();
		baddie.dispose();
		hitSound.dispose();
		levelUpSound.dispose();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(1f, 1f, 1f, 0.5f);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		camera.update();
		
		if(gameOver){
			batch.setProjectionMatrix(camera.combined);
			batch.begin();
			gofont.draw(batch, "Game Over!", CAMERA_WIDTH / 2 - gofont.getBounds("Game Over!").width/2, CAMERA_HEIGHT / 2 + 100);
			font.draw(batch, "Score : " + score, CAMERA_WIDTH / 2 - font.getBounds("Score : XX").width/2, CAMERA_HEIGHT / 2);
			batch.end();
			
			/** Input **/
			if(Gdx.input.isTouched() ) {
				score = 0;
				lives = 5;
				gameOver = false;
				startTime = TimeUtils.millis();
				baddies = new Array<Rectangle>();
				bullets = new Array<Bullet2D>();
			}
			
		}else{
		
			/** Render **/
			batch.setProjectionMatrix(camera.combined);
			batch.begin();
			font.draw(batch, "Score: " + score, 10, CAMERA_HEIGHT - 10);
			font.draw(batch, "Lives: " + lives, CAMERA_WIDTH - 120, CAMERA_HEIGHT - 10);
			for(Rectangle b : baddies){
				batch.draw(baddie, b.x, b.y);
			}
			for(Bullet2D b : bullets){
				batch.draw(bullet, b.rect.x, b.rect.y);
			}
			batch.draw(tower, rect_tower.x, rect_tower.y);
			batch.end();
			
			Iterator<Bullet2D> iter = bullets.iterator();
			while(iter.hasNext()){
				Bullet2D b = iter.next();
				b.rect.y += Gdx.graphics.getDeltaTime() * b.slope.y;
				b.rect.x += Gdx.graphics.getDeltaTime() * b.slope.x;
				if(b.rect.y + 16 < 0){
					iter.remove();
					//Log.i("", "Bullet Removed");
				}else{
					Iterator<Rectangle> iterb = baddies.iterator();
					while(iterb.hasNext()){
						Rectangle baddie = iterb.next();
						if(baddie.overlaps(b.rect)){
							hitSound.play();
							/*This somehow is crashing the game*/
							iter.remove();
							/*                                 */
							iterb.remove();
							score++;
							levelUp++;
							if(levelUp == 10){
								lives++;
								difficultyMultiplier++;
								levelUpSound.play();
								levelUp = 0;
							}
							//Log.i("score", score +"");
						}
					}
				}
			}
			
			Iterator<Rectangle> iterb = baddies.iterator();
			while(iterb.hasNext()){
				Rectangle b = iterb.next();
				b.y += Gdx.graphics.getDeltaTime() * (200 + ( (TimeUtils.millis() - startTime )/ 1000) );
				//Log.i("Baddie Speed Boost", ( (TimeUtils.millis() - startTime )/ 1000) +"");
				if(b.y - 64 > rect_tower.y){
					iterb.remove();
					//Log.i("", "Lost a life!");
					lives--;
					if(lives <= 0)
						gameOver = true;
				}
			}
			//Spawn baddies
			if(TimeUtils.nanoTime() - lastBaddieTime > 3000000000l){
				for(int x = 0; x < difficultyMultiplier; x++){
					spawnBaddie();
				}
				//Log.i("", "Baddie Spawned");
			}
			
			/** Input **/
			if(Gdx.input.isTouched() ) {
				Vector3 touchPos = new Vector3();
				touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
				camera.unproject(touchPos);
				if(TimeUtils.nanoTime() - lastShotTime > 200000000l){				
					spawnBullet(touchPos.x, touchPos.y);
				}
				//Log.i("Touch Pos", "[X: " + touchPos.x + ", Y: " + touchPos.y + "]");
			}
		}
		
	}

	@Override
	public void resize(int arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}
	
	private void spawnBullet(float dirX, float dirY ){
		Bullet2D b = new Bullet2D();
		
		Rectangle rect_b = new Rectangle();
		rect_b.width = 16;
		rect_b.height = 16;		
		rect_b.x = (CAMERA_WIDTH / 2) - (rect_b.width / 2);
		rect_b.y = (CAMERA_HEIGHT) - (rect_tower.height);
		b.rect = rect_b;
		
		Vector2 slope = new Vector2((dirX - rect_b.x), (dirY - rect_b.y));
		slope.clamp(500, 500);
		
		b.slope = slope;
		
		bullets.add(b);
		lastShotTime = TimeUtils.nanoTime();
	}
	
	private void spawnBaddie(){
		Rectangle b = new Rectangle();
		b.x = MathUtils.random(0, CAMERA_WIDTH - 64);
		b.y = 0 + 64;
		b.width = 64;
		b.height = 64;		
		baddies.add(b);		
		lastBaddieTime = TimeUtils.nanoTime();
		
	}

}
