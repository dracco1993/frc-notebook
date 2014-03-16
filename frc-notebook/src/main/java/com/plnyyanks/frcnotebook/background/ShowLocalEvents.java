package com.plnyyanks.frcnotebook.background;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.plnyyanks.frcnotebook.Constants;
import com.plnyyanks.frcnotebook.R;
import com.plnyyanks.frcnotebook.activities.StartActivity;
import com.plnyyanks.frcnotebook.activities.ViewEvent;
import com.plnyyanks.frcnotebook.adapters.ActionBarCallback;
import com.plnyyanks.frcnotebook.adapters.ListViewArrayAdapter;
import com.plnyyanks.frcnotebook.datatypes.Event;
import com.plnyyanks.frcnotebook.datatypes.ListElement;
import com.plnyyanks.frcnotebook.datatypes.ListHeader;
import com.plnyyanks.frcnotebook.datatypes.ListItem;
import com.plnyyanks.frcnotebook.dialogs.DeleteDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by phil on 2/22/14.
 */
public class ShowLocalEvents extends AsyncTask<Activity,String,String> {

    private Activity parentActivity;
    private ListViewArrayAdapter adapter;
    private ListView eventList;
    private Object mActionMode;
    private int selectedItem=-1;
    ArrayList<String> finalKeys = new ArrayList<String>();
    ArrayList<ListItem> finalEvents = new ArrayList<ListItem>();


    @Override
    protected String doInBackground(Activity... activities) {
        parentActivity = activities[0];
        final List<Event> storedEvents = StartActivity.db.getAllEvents();
        Collections.sort(storedEvents);

        eventList = (ListView) parentActivity.findViewById(R.id.event_list);
        eventList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        Event e;
        int eventWeek = Integer.parseInt(Event.weekFormatter.format(new Date())),
            currentWeek;
        for(int i=0;i<storedEvents.size();i++){
            e=storedEvents.get(i);
            currentWeek = e.getCompetitionWeek();
            if(eventWeek != currentWeek){
                finalEvents.add(new ListHeader(e.getEventYear()+ " Week "+currentWeek));
                finalKeys.add(e.getEventYear()+"_week"+currentWeek);
            }
            eventWeek = currentWeek;

            finalEvents.add(new ListElement(e.getEventName(),e.getEventKey()));
            finalKeys.add(e.getEventKey());
        }
        if(storedEvents.size()==0){
            finalEvents.add(new ListElement(parentActivity.getString(R.string.no_events_message),"-1"));
            finalKeys.add("-1");
        }

        parentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter = new ListViewArrayAdapter(parentActivity,finalEvents,finalKeys);
                eventList.setAdapter(adapter);
                if(storedEvents.size()!=0){
                    eventList.setOnItemClickListener(new ClickListener());
                    eventList.setOnItemLongClickListener(new LongClickListener());
                }
                //eventList.setOnItemSelectedListener(new SelectedListener());
            }
        });

        return null;
    }




    private class ClickListener implements ListView.OnItemClickListener{

        public ClickListener(){
            super();
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Log.d(Constants.LOG_TAG, "Item click: " + i + ", selected: " + selectedItem);
            ListItem item = adapter.values.get(i);
            if (item instanceof ListHeader) return;
            String eventKey = finalKeys.get(i);
            ViewEvent.setEvent(eventKey);
            Intent intent = new Intent(parentActivity, ViewEvent.class);
            parentActivity.startActivity(intent);

        }
    }

    private class LongClickListener implements ListView.OnItemLongClickListener {

        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
            Log.d(Constants.LOG_TAG, "Item Long Click: " + i);
            ListItem item =  adapter.values.get(i);
            if(item instanceof ListHeader) return false;

            eventList.setOnItemClickListener(null);
            item.setSelected(true);
            view.setSelected(true);
            adapter.notifyDataSetChanged();
            selectedItem = i;
            // start the CAB using the ActionMode.Callback defined above
            mActionMode = parentActivity.startActionMode(mActionModeCallback);
            return false;
        }
    }

    private class SelectedListener implements ListView.OnItemSelectedListener{

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            Log.d(Constants.LOG_TAG,"Item Selected: "+i);
            view.setBackgroundResource(android.R.color.holo_blue_light);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            adapterView.setBackgroundResource(android.R.color.transparent);
        }
    }

    private ActionMode.Callback mActionModeCallback = new ActionBarCallback() {
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    confirmAndDelete(selectedItem);
                    // the Action was executed, close the CAB
                    selectedItem = -1;
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            Log.d(Constants.LOG_TAG,"Destroy CAB");
            mActionMode = null;
            eventList.setOnItemClickListener(new ClickListener());
            eventList.requestFocusFromTouch();
            eventList.clearChoices();
            adapter.notifyDataSetChanged();
        }

        private void confirmAndDelete(final int item){
            DialogInterface.OnClickListener deleter =
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //delete the event now
                            Toast.makeText(parentActivity, "Deleting " + finalKeys.get(item), Toast.LENGTH_SHORT).show();
                            new DeleteEvent(parentActivity).execute(finalKeys.get(item));
                            adapter.removeAt(item);
                            adapter.notifyDataSetChanged();
                            dialog.cancel();
                        }
                    };
            new DeleteDialog(parentActivity.getString(R.string.delete_event_message)+finalKeys.get(item)+"?",deleter)
                    .show(parentActivity.getFragmentManager(), "delete_event");
        }
    };
}

