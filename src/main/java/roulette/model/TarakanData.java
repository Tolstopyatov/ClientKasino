package roulette.model;

import java.util.List;

public class TarakanData {
    private String id;
    private double x;
    private double y;
    private double speed;
    private List<Point> route;
    private boolean finished;

    public TarakanData() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public List<Point> getRoute() { return route; }
    public void setRoute(List<Point> route) { this.route = route; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }

    public static class Point {
        private double x;
        private double y;
        public Point() {}
        public Point(double x, double y) { this.x = x; this.y = y; }
        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
    }
}