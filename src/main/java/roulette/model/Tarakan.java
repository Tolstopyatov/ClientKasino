package roulette.model;

import java.util.ArrayList;
import java.util.List;

public class Tarakan {
    private String id;
    private double x;
    private double y;
    private double speed;
    private List<Point> route;
    private int currentRoutePointIndex;
    private boolean finished;

    // Пустой конструктор для Gson
    public Tarakan() {
    }

    public Tarakan(int id, double startX, double startY, double speed) {
        this.id = String.valueOf(id);
        this.x = startX;
        this.y = startY;
        this.speed = speed;
        this.route = new ArrayList<>();
        this.currentRoutePointIndex = 0;
        this.finished = false;
    }

    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSpeed() { return speed; }
    public List<Point> getRoute() { return route; }
    public void setRoute(List<Point> route) { this.route = route; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }

    public void updatePosition(double deltaTime) {
        if (finished || route.isEmpty() || currentRoutePointIndex >= route.size()) {
            return;
        }

        Point target = route.get(currentRoutePointIndex);
        double dx = target.getX() - x;
        double dy = target.getY() - y;
        double distanceToTarget = Math.sqrt(dx * dx + dy * dy);

        double distanceMoved = speed * deltaTime;

        if (distanceMoved >= distanceToTarget) {
            x = target.getX();
            y = target.getY();
            currentRoutePointIndex++;
            if (currentRoutePointIndex >= route.size()) {
                finished = true;
            }
        } else {
            x += dx / distanceToTarget * distanceMoved;
            y += dy / distanceToTarget * distanceMoved;
        }
    }

    public static class Point {
        private double x;
        private double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() { return x; }
        public double getY() { return y; }
    }
}