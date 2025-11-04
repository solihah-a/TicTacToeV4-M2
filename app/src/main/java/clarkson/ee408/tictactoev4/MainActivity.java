package clarkson.ee408.tictactoev4;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import com.google.gson.Gson;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

// Imports for Tasks 4, 7, 8
import clarkson.ee408.tictactoev4.client.AppExecutors;
import clarkson.ee408.tictactoev4.client.SocketClient;
import clarkson.ee408.tictactoev4.socket.Request;
import clarkson.ee408.tictactoev4.socket.RequestType;
import clarkson.ee408.tictactoev4.socket.GamingResponse;
import clarkson.ee408.tictactoev4.socket.ResponseStatus;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int DELAY_MS = 1000; // 1 second delay for polling

    // Attributes from Task 5
    private TicTacToe tttGame;
    private Button [][] buttons;
    private TextView status;

    // Attributes from Task 2
    private Gson gson;

    // Attributes for Task 8: Periodic move request
    private Handler handler;
    private Runnable moveRequestRunnable;
    private boolean shouldRequestMove = false; // Flag to control polling

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        // Task 5: Initialize game with a placeholder Player ID (e.g., 1)
        // This ID should be set properly after receiving server connection info in a later task.
        tttGame = new TicTacToe(1);
        buildGuiByCode( );

        // Task 2: Initialize Gson
        this.gson = new Gson();

        // --- Task 8: Handler Initialization ---
        handler = new Handler(Looper.getMainLooper());

        // Define the Runnable action: requestMove()
        moveRequestRunnable = new Runnable() {
            @Override
            public void run() {
                if (shouldRequestMove) { // Check the flag before making the request
                    requestMove();
                }
                // Schedule the runnable to run again after a delay
                handler.postDelayed(this, DELAY_MS);
            }
        };

        // --- Start the polling mechanism ---
        // Starts the Handler loop immediately. Polling logic is controlled by shouldRequestMove.
        handler.post(moveRequestRunnable);

        // Temporary: Set to true to start polling immediately for testing REQUEST_MOVE logic
        // This will be managed by game connection logic later.
        setShouldRequestMove(true);
    }

    // Task 8: New method to encapsulate the network request for an opponent's move
    private void requestMove() {
        Log.d(TAG, "Attempting to request move from server...");

        // 1. Get the Network Executor and run network operation off the main thread
        AppExecutors.getInstance().networkIO().execute(() -> {

            // 2. Build the REQUEST_MOVE request
            Request request = new Request(RequestType.REQUEST_MOVE, null);

            // 3. Send the request and wait for the GamingResponse
            SocketClient client = SocketClient.getInstance();
            GamingResponse response = client.sendRequest(request, GamingResponse.class);

            // 4. Update the UI on the Main Thread based on the response
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (response == null || response.getStatus() == ResponseStatus.FAILURE) {
                    Log.e(TAG, "RequestMove failed: " + (response != null ? response.getMessage() : "Null response."));
                    // Handle server disconnection or error
                    return;
                }

                // Response Status is SUCCESS
                Integer move = response.getMove();

                if (move != null && move != -1) {
                    // Opponent made a move!
                    Log.i(TAG, "Received opponent move: " + move);

                    // Convert move (0-8) to (row, col)
                    int row = move / TicTacToe.SIDE;
                    int col = move % TicTacToe.SIDE;

                    // Simulate opponent playing the move and update UI
                    update(row, col);

                    // After receiving a move, it's now THIS player's turn, so stop polling
                    setShouldRequestMove(false);
                } else {
                    // Move is -1 or null, meaning no new move yet. Polling continues.
                    Log.d(TAG, "No new move available. Polling again in " + DELAY_MS + "ms.");
                }
            });
        });
    }

    // New setter method to safely control the polling flag
    public void setShouldRequestMove(boolean state) {
        if (this.shouldRequestMove != state) {
            Log.d(TAG, "shouldRequestMove changed to: " + state);
        }
        this.shouldRequestMove = state;
    }

    // --- Lifecycle Methods for Handler Control ---

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null && moveRequestRunnable != null) {
            // Remove all callbacks when activity is paused/hidden
            handler.removeCallbacks(moveRequestRunnable);
            Log.d(TAG, "Polling stopped.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (handler != null && moveRequestRunnable != null) {
            // Resume polling when activity is foregrounded
            handler.post(moveRequestRunnable);
            Log.d(TAG, "Polling resumed.");
        }
    }

    // --- UI/Game Logic (Existing Methods) ---

    public void buildGuiByCode( ) {
        // Get width of the screen
        Point size = new Point( );
        getWindowManager( ).getDefaultDisplay( ).getSize( size );
        int w = size.x / TicTacToe.SIDE;

        // Create the layout manager as a GridLayout
        GridLayout gridLayout = new GridLayout( this );
        gridLayout.setColumnCount( TicTacToe.SIDE );
        gridLayout.setRowCount( TicTacToe.SIDE + 2 );

        // Create the buttons and add them to gridLayout
        buttons = new Button[TicTacToe.SIDE][TicTacToe.SIDE];

        gridLayout.setUseDefaultMargins(true);

        for( int row = 0; row < TicTacToe.SIDE; row++ ) {
            for( int col = 0; col < TicTacToe.SIDE; col++ ) {
                buttons[row][col] = new Button( this );
                buttons[row][col].setTextSize( ( int ) ( w * .2 ) );
                // Use MainActivity as the OnClickListener (implements View.OnClickListener)
                buttons[row][col].setOnClickListener( this );

                GridLayout.LayoutParams bParams = new GridLayout.LayoutParams();
                //ensures vertical gap
                bParams.topMargin = 10;
                bParams.bottomMargin = 10;
                //ensures horizontal gap
                bParams.leftMargin = 10;
                bParams.rightMargin = 10;
                //account for margin change
                bParams.width=w-20;
                bParams.height=w-20;
                buttons[row][col].setLayoutParams(bParams);
                // Assign a unique ID based on position (0 to 8) for easier reference
                buttons[row][col].setId(row * TicTacToe.SIDE + col);
                gridLayout.addView( buttons[row][col]);
            }
        }

        // set up layout parameters of 4th row of gridLayout
        status = new TextView( this );
        GridLayout.Spec rowSpec = GridLayout.spec( TicTacToe.SIDE, 2 );
        GridLayout.Spec columnSpec = GridLayout.spec( 0, TicTacToe.SIDE );
        GridLayout.LayoutParams lpStatus
                = new GridLayout.LayoutParams( rowSpec, columnSpec );
        status.setLayoutParams( lpStatus );

        // set up status' characteristics
        status.setWidth( TicTacToe.SIDE * w );
        status.setHeight( w * 2 );
        status.setGravity( Gravity.CENTER );
        status.setBackgroundColor( Color.BLUE );
        status.setTextSize( ( int ) ( w * .15 ) );
        status.setText( tttGame.result( ) );

        gridLayout.addView( status );

        // Set gridLayout as the View of this Activity
        setContentView( gridLayout );
    }

    public void update( int row, int col ) {
        int play = tttGame.play( row, col );
        if( play == 1 )
            buttons[row][col].setText( "X" ); // Changed to X/O for clarity
        else if( play == 2 )
            buttons[row][col].setText( "O" ); // Changed to X/O for clarity

        if( tttGame.isGameOver( ) ) {
            status.setBackgroundColor( Color.YELLOW);
            enableButtons( false );
            status.setText( tttGame.result( ) );
            showNewGameDialog( );   // offer to play again
        } else {
            // If game is not over, update status text to show whose turn it is
            status.setText(tttGame.result());
        }
    }

    public void enableButtons( boolean enabled ) {
        for( int row = 0; row < TicTacToe.SIDE; row++ )
            for( int col = 0; col < TicTacToe.SIDE; col++ )
                buttons[row][col].setEnabled( enabled );
    }

    public void resetButtons( ) {
        for( int row = 0; row < TicTacToe.SIDE; row++ )
            for( int col = 0; col < TicTacToe.SIDE; col++ )
                buttons[row][col].setText( "" );
    }

    public void showNewGameDialog( ) {
        AlertDialog.Builder alert = new AlertDialog.Builder( this );
        alert.setTitle( "TicTacToe Game" );
        alert.setMessage( "Play again?" );
        PlayDialog playAgain = new PlayDialog( );
        alert.setPositiveButton( "YES", playAgain );
        alert.setNegativeButton( "NO", playAgain );
        alert.show( );
    }

    // Task 6: Implementing the required onClick method
    @Override
    public void onClick( View v ) {
        Log.d(TAG, "Button clicked with ID: " + v.getId());

        // 1. Find the row and column of the clicked button
        int row = v.getId() / TicTacToe.SIDE;
        int column = v.getId() % TicTacToe.SIDE;

        // 2. Check if it's the player's turn (handled by TicTacToe.play() logic in later steps)
        // For now, allow the move and update the board locally
        update( row, column );

        // 3. TODO: In later tasks, this is where we will send the move to the server.
    }

    private class PlayDialog implements DialogInterface.OnClickListener {
        public void onClick( DialogInterface dialog, int id ) {
            if( id == -1 ) /* YES button */ {
                tttGame.resetGame( );
                enableButtons( true );
                resetButtons( );
                status.setBackgroundColor( Color.BLUE );
                status.setText( tttGame.result( ) );

                // TODO: In later tasks, this is where we will inform the server we are ready for a new game.
            }
            else if( id == -2 ) // NO button
                MainActivity.this.finish( );
        }
    }
}