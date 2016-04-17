package ld35;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class MiniPhysic {
	public static class Body{
		public float x = 0f;
		public float y = 0f;
		public float px = 0f;
		public float py = 0f;
		public float ax = 0f;
		public float ay = 0f;
		public float fx = 0f;
		public float fy = 0f;
		public float radius = 1f;

		public Body(float x, float y, float radius) {
			this.x = x;
			this.y = y;
			this.px = x;
			this.py = y;
			this.ax = 0;
			this.ay = 0;
			this.radius = radius;
		}

		void accelerate(float delta){
			this.x += this.ax * delta * delta;
			this.y += this.ay * delta * delta;
			this.ax = 0;
			this.ay = 0;
		}
		void inertia(float delta){
			float x = this.x*2 - this.px;
			float y = this.y*2 - this.py;
			this.px = this.x;
			this.py = this.y;
			this.x = x;
			this.y = y;
		}
	}

	public static class Simulation implements Runnable{
		public final List<Body> bodies = new LinkedList<Body>();
		public float width;
		public float height;
		public float damping = 0.1f;
		ScheduledExecutorService executor;
		/*
	        def collide(boolean preserve_impulse){
	            for(var i=0, l=bodies.length; i<l; i++){
	                var body1 = bodies[i];
	                for(var j=i+1; j<l; j++){
	                    var body2 = bodies[j];
	                    var x = body1.x - body2.x;
	                    var y = body1.y - body2.y;
	                    var slength = x*x+y*y;
	                    var length = Math.sqrt(slength);
	                    var target = body1.radius + body2.radius;

	                    if(length < target){
	                        var v1x = body1.x - body1.px;
	                        var v1y = body1.y - body1.py;
	                        var v2x = body2.x - body2.px;
	                        var v2y = body2.y - body2.py;

	                        var factor = (length-target)/length;
	                        body1.x -= x*factor*0.5;
	                        body1.y -= y*factor*0.5;
	                        body2.x += x*factor*0.5;
	                        body2.y += y*factor*0.5;

	                        if(preserve_impulse){
	                            var f1 = (damping*(x*v1x+y*v1y))/slength;
	                            var f2 = (damping*(x*v2x+y*v2y))/slength;

	                            v1x += f2*x-f1*x;
	                            v2x += f1*x-f2*x;
	                            v1y += f2*y-f1*y;
	                            v2y += f1*y-f2*y;

	                            body1.px = body1.x - v1x;
	                            body1.py = body1.y - v1y;
	                            body2.px = body2.x - v2x;
	                            body2.py = body2.y - v2y;
	                        }
	                    }
	                }
	            }
	        }
		 */

		void border_collide_preserve_impulse(){
			for(Body body: bodies){
				float radius = body.radius;
				float x = body.x;
				float y = body.y;

				if(x-radius < -width){
					float vx = (body.px - body.x)*damping;
					body.x = radius-width;
					body.px = body.x - vx;
				}
				else if(x + radius > width){
					float vx = (body.px - body.x)*damping;
					body.x = width-radius;
					body.px = body.x - vx;
				}
				if(y-radius < -height){
					float vy = (body.py - body.y)*damping;
					body.y = radius-height;
					body.py = body.y - vy;
				}
				else if(y + radius > height){
					float vy = (body.py - body.y)*damping;
					body.y = height-radius;
					body.py = body.y - vy;
				}
			}
		}

		void border_collide(){
			for(Body body: bodies){
				float radius = body.radius;
				float x = body.x;
				float y = body.y;

				if(x-radius < -width){
					body.x = radius-width;
				}
				else if(x + radius > width){
					body.x = width-radius;
				}
				if(y-radius < -height){
					body.y = radius-height;
				}
				else if(y + radius > height){
					body.y = height-radius;
				}
			}
		}

		void applyForce(){
			for(Body body: bodies){
				body.ax += body.fx;
				body.ay += body.fy;
			}
		}

		void accelerate(float delta){
			for(Body body: bodies){
				body.accelerate(delta);
			}
		}

		void inertia(float delta){
			for(Body body: bodies){
				body.inertia(delta);
			}
		}

		public void run(){
			int steps = 2;
			//float delta = 1f/((float)steps);
			float delta = 1f/30f;
			for(int i=0; i<steps; i++){
				applyForce();
				accelerate(delta);
				//collide(false);
				border_collide();
				inertia(delta);
				//collide(true);
				border_collide_preserve_impulse();
			}
		}

		public void start(){
			stop();
			executor = Executors.newSingleThreadScheduledExecutor();
			executor.scheduleAtFixedRate(this, 0, 1000/30, TimeUnit.MILLISECONDS);
		}

		public void stop(){
			if (executor != null) {
				executor.shutdownNow();
				executor = null;
			}
		}
	}
}
