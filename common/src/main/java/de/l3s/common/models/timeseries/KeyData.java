package de.l3s.common.models.timeseries;

public final class KeyData {
	
	public final TimeseriesDataPoint dataPoint;
	public final TimeseriesKey key;
	
	public KeyData(TimeseriesDataPoint dataPoint, TimeseriesKey key) {
		this.dataPoint = dataPoint;
		this.key = key;
	}
	
	public static KeyData make (TimeseriesDataPoint dataPoint, TimeseriesKey key) {
		return new KeyData(dataPoint, key);
	}
	
	public boolean equals(Object o) {
		if (o == null || o.getClass() != this.getClass()) { return false; }
		KeyData that = (KeyData) o;
		return (dataPoint == null ? that.dataPoint == null : dataPoint.equals(that.dataPoint))
		&& (key == null ? that.key == null : key.equals(that.key));
	}

}
