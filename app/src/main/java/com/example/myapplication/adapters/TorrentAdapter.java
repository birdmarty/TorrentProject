package com.example.myapplication.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.parsing.SearchResult;
import com.example.myapplication.services.TorrentDownloadService;

import java.util.List;

public class TorrentAdapter extends RecyclerView.Adapter<TorrentAdapter.ViewHolder> {

    private final List<SearchResult> results;
    private final Context context; // Add context field
    private final RecyclerviewListener listener;

    public interface RecyclerviewListener {
        void onItemClick(int position);
    }

    public TorrentAdapter(Context context, RecyclerviewListener listener, List<SearchResult> results) {
        this.context = context; // Initialize context
        this.listener = listener;
        this.results = results;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_torrent, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = results.get(position);
        holder.title.setText(result.getTitle());
        holder.seeds.setText(result.getSeeds());
        holder.leeches.setText(result.getLeeches());
        holder.size.setText(result.getSize());
        holder.website.setText(result.getWebsite());
        holder.link.setText(result.getLink());
        holder.infohash.setText(result.getInfoHash());

        holder.itemView.setOnClickListener(v -> {
            Log.d("TorrentAdapter", "Torrent clicked: " + result.getTitle());

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(result.getTitle())
                    .setItems(new String[]{"Watch", "Download"}, (dialog, which) -> {
                        Log.d("TorrentAdapter", "Selected option: " + (which == 0 ? "Watch" : "Download"));

                        Intent serviceIntent = new Intent(context, TorrentDownloadService.class);
                        serviceIntent.putExtra("infoHash", result.getInfoHash());
                        serviceIntent.putExtra("torrentLink", result.getLink());
                        serviceIntent.putExtra("title", result.getTitle());
                        serviceIntent.putExtra("watchMode", which == 0);
                        Log.d("TorrentAdapter", "Starting service with infoHash: " + result.getInfoHash());

                        context.startService(serviceIntent);
                    })
                    .show();

        });

    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, seeds, leeches, size, website, link, infohash;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            seeds = itemView.findViewById(R.id.text_seeds);
            leeches = itemView.findViewById(R.id.text_leeches);
            size = itemView.findViewById(R.id.text_size);
            website = itemView.findViewById(R.id.text_website);
            link = itemView.findViewById(R.id.text_link);
            infohash = itemView.findViewById(R.id.text_infohash);
        }
    }
}
