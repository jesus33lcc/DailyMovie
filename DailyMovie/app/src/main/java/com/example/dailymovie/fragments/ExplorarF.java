package com.example.dailymovie.fragments;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailymovie.R;
import com.example.dailymovie.adapters.MovieAdapter;
import com.example.dailymovie.client.RetrofitClient;
import com.example.dailymovie.client.WebService;
import com.example.dailymovie.client.response.MoviesResponse;
import com.example.dailymovie.models.MovieModel;
import com.example.dailymovie.utils.Constantes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExplorarF extends Fragment {
    private View view;
    private RecyclerView moviesRecyclerView;
    MovieAdapter movieAdapter;
    List<MovieModel> listMovies;
    Toolbar toolbarSearch;
    private SearchView searchViewExplorar;

    public ExplorarF() { } // Se requiere de un constructor vacio.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_explorar,container,false);

        // Sobre el RecyclerView
        moviesRecyclerView = view.findViewById(R.id.rvListaBusqueda);
        moviesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Insertar Libros en lista.
        //List<MovieModel> listMoviewEmpty = new ArrayList<>();
        //initialize_ListFillMovie(listMoviewEmpty);
        //buscar();

        // Sobre el Toolbar
        toolbarSearch = view.findViewById(R.id.myToolbarExplorer);

        // Sobre el SearchView
        MenuItem searchItem = toolbarSearch.getMenu().findItem(R.id.action_searchExplore);
        searchViewExplorar = (SearchView) searchItem.getActionView();
        searchViewExplorar.setQueryHint("Buscar titulo...");

        // Configuracion color del SearchView
        int colorNegro = ContextCompat.getColor(requireContext(), android.R.color.black);
        EditText searchEditText = searchViewExplorar.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setTextColor(colorNegro);
        searchEditText.setHintTextColor(colorNegro);
        Drawable iconoSearch = searchItem.getIcon();
        iconoSearch.setColorFilter(colorNegro, PorterDuff.Mode.SRC_ATOP);

        searchViewExplorar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Cuando el usuario confirma la búsqueda, esconder el teclado virtual podría ser útil.
                searchViewExplorar.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.isEmpty() && newText.length() > 2) { // Considerar la búsqueda después de 2 caracteres.
                    buscar(newText);
                } else if (newText.isEmpty()) {
                    clearResults(); // Limpiar resultados si el texto de búsqueda está vacío.
                }
                return true;
            }
        });


        return view;
    }


    private void buscar(String query) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            WebService webService = RetrofitClient.INSTANCE.getWebService();
            Call<MoviesResponse> call = webService.searchMovies(query, Constantes.API_KEY, true, "en_US", 1);
            call.enqueue(new Callback<MoviesResponse>() {
                @Override
                public void onResponse(Call<MoviesResponse> call, Response<MoviesResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        updateUIWithResults(response.body().getResults());
                    } else {
                        showToast("Error en la respuesta");
                    }
                }

                @Override
                public void onFailure(Call<MoviesResponse> call, Throwable t) {
                    showToast("Fallo en la conexión");
                }
            });
        });
    }

    private void updateUIWithResults(List<MovieModel> results) {
        new Handler(Looper.getMainLooper()).post(() -> {
            initialize_ListFillMovie(results);
        });
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
    }


    private void fillRecycleList(){
        Handler handler=new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                initialize_ListFillMovie(listMovies);
            }
        });
    }

    public void initialize_ListFillMovie(List<MovieModel> listMovieFill){
        if (movieAdapter == null) {
            movieAdapter = new MovieAdapter(new ArrayList<>());
            moviesRecyclerView.setAdapter(movieAdapter);
        }
        movieAdapter.updateMoviesList(new ArrayList<>(listMovieFill));
    }
    private void clearResults() {
        if (movieAdapter != null) {
            movieAdapter.updateMoviesList(new ArrayList<>());
        }
    }
}