package es.android.TurnosAndroid.views.myevents;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import es.android.TurnosAndroid.R;
import es.android.TurnosAndroid.model.Event;

import java.util.ArrayList;

/**
 * User: Jesús
 * Date: 23/12/13
 */
public class MyEventsAdapter extends BaseAdapter {
  private final Context          context;
  private       ArrayList<Event> events;

  public MyEventsAdapter(Context context, ArrayList<Event> events) {
    this.context = context;
    this.events = events;
  }

  public void setMyEvents(ArrayList<Event> events) {
    this.events = events;
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return events.size();
  }

  @Override
  public Object getItem(int position) {
    return events.get(position);
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View view;

    // TODO implement ViewHolder
    if (convertView != null) {
      view = convertView;
    } else {
      view = LayoutInflater.from(context).inflate(R.layout.my_events_row, parent, false);
    }

    Event event = events.get(position);
    TextView eventTitle = (TextView) view.findViewById(R.id.my_events_row_event_title);
    eventTitle.setText(event.getName());

    return view;
  }
}