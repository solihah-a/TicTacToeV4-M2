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

    // Attributes
    private TicTacToe tttGame;
    private Button [][] buttons;
    private TextView status;
    private Gson gson;

    // Attributes for Task 8: Periodic move request
    private Handler handler;
    private Runnable moveRequestRunnable;
    private boolean shouldRequestMove = false; // Flag to control polling

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        // Task 5: Initialize game with a placeholder Player ID (e.g., 1)
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
                if (shouldRequestMove) {
                    requestMove();
                }
                // Schedule the runnable to run again after a delay
                handler.postDelayed(this, DELAY_MS);
            }
        };

        // --- Start the polling mechanism ---
        handler.post(moveRequestRunnable);

        // Task 9: Initial call to set status and polling flag based on turn (Player 1 starts)
        updateTurnStatus();
    }

    /**
     * Task 9: Updates the status text, button state, and polling flag based on whose turn it is.
     */
    private void updateTurnStatus() {
        if (tttGame.isGameOver()) {
            return; // Don't update turn status if the game is over
        }

        if (tttGame.getTurn() == tttGame.getPlayer()) {
            // It is this player's turn
            status.setText("Your Turn");
            enableButtons(true);
            setShouldRequestMove(false); // Stop polling
            Log.d(TAG, "Status: Your Turn. Polling stopped.");
        } else {
            // It is the opponent's turn
            status.setText("Waiting for Opponent");
            enableButtons(false);
            setShouldRequestMove(true); // Start polling
            Log.d(TAG, "Status: Waiting for Opponent. Polling started.");
        }
    }


    /**
     * Task 10: Sends a REQUEST_MOVE to the server and processes the response.
     */
    private void requestMove() {
        Log.d(TAG, "Polling server for opponent's move...");

        // Run network operation off the main thread
        AppExecutors.getInstance().networkIO().execute(() -> {

            // 2. Build the REQUEST_MOVE request
            Request request = new Request(RequestType.REQUEST_MOVE, null);

            // 3. Send the request and wait for the GamingResponse
            SocketClient client = SocketClient.getInstance();
            GamingResponse response = client.sendRequest(request, GamingResponse.class);

            // 4. Update the UI on the Main Thread based on the response
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (response == null) {
                    Log.e(TAG, "RequestMove failed: Null response from server (is server running?).");
                    return;
                }

                if (response.getStatus() == ResponseStatus.FAILURE) {
                    Log.e(TAG, "RequestMove failed: " + response.getMessage());
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

                    // Update game state and UI
                    update(row, col);
                } else {
                    // Move is -1 or null, meaning no new move yet. Polling continues automatically via the Handler.
                    Log.d(TAG, "No new move available.");
                }
            });
        });
    }

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
            handler.removeCallbacks(moveRequestRunnable);
            Log.d(TAG, "Polling stopped.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (handler != null && moveRequestRunnable != null) {
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
                // Use MainActivity as the OnClickListener
                buttons[row][col].setOnClickListener( this );

                GridLayout.LayoutParams bParams = new GridLayout.LayoutParams();
                bParams.topMargin = 10;
                bParams.bottomMargin = 10;
                bParams.leftMargin = 10;
                bParams.rightMargin = 10;
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
        status.setText( tttGame.result( ) ); // Will be overwritten by updateTurnStatus()

        gridLayout.addView( status );

        // Set gridLayout as the View of this Activity
        setContentView( gridLayout );
    }

    public void update( int row, int col ) {
        int play = tttGame.play( row, col );

        if (play == 0) return; // Invalid move (square taken)

        if( play == 1 )
            buttons[row][col].setText( "X" );
        else if( play == 2 )
            buttons[row][col].setText( "O" );

        // Task 9: Update turn status immediately after a move is recorded
        updateTurnStatus();

        if( tttGame.isGameOver( ) ) {
            status.setBackgroundColor( Color.YELLOW);
            enableButtons( false );
            status.setText( tttGame.result( ) );
            setShouldRequestMove(false); // Stop polling when game is over
            showNewGameDialog( );   // offer to play again
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

    @Override
    public void onClick( View v ) {
        Log.d(TAG, "Button clicked with ID: " + v.getId());

        // 1. Check if it is currently this player's turn
        if (tttGame.getTurn() != tttGame.getPlayer()) {
            Log.w(TAG, "Not your turn! Button click ignored.");
            return;
        }

        // 2. Find the row and column of the clicked button
        int moveId = v.getId();
        int row = moveId / TicTacToe.SIDE;
        int column = moveId % TicTacToe.SIDE;

        // 3. Check if the spot is already taken by using the public getter
        if (tttGame.getCell(row, column) != 0) {
            Log.w(TAG, "Spot already taken.");
            return;
        }

        // 4. Update the board locally
        update( row, column );

        // 5. TODO: In later tasks, this is where we will send the move to the server.
    }

    private class PlayDialog implements DialogInterface.OnClickListener {
        public void onClick( DialogInterface dialog, int id ) {
            if( id == -1 ) /* YES button */ {
                tttGame.resetGame( );
                enableButtons( true );
                resetButtons( );
                status.setBackgroundColor( Color.BLUE );

                // Task 9: Call updateTurnStatus to reset UI/polling logic
                updateTurnStatus();

                // TODO: In later tasks, this is where we will inform the server we are ready for a new game.
            }
            else if( id == -2 ) // NO button
                MainActivity.this.finish( );
        }
    }
}