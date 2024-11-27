package KilimanJARo.P2P.user;

public record Coordinates(double latitude, double longitude) {

  public double distanceTo(Coordinates other) {
    final double EARTH_RADIUS = 6371000; // in meters
    double latDistance = Math.toRadians(other.latitude - this.latitude);
    double lonDistance = Math.toRadians(other.longitude - this.longitude);
    double a =
        Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(this.latitude))
                * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS * c;
  }

  public double[] toXYZ() {
    final double EARTH_RADIUS = 6371000; // in meters
    double latRad = Math.toRadians(latitude);
    double lonRad = Math.toRadians(longitude);

    double x = EARTH_RADIUS * Math.cos(latRad) * Math.cos(lonRad);
    double y = EARTH_RADIUS * Math.cos(latRad) * Math.sin(lonRad);
    double z = EARTH_RADIUS * Math.sin(latRad);

    return new double[] {x, y, z};
  }
}
