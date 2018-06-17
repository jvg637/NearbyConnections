package androidthings.example.org.nearbyconnections;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    // Consejo: utiliza como SERVICE_ID el nombre de tu paquete
    private static final String SERVICE_ID = "androidthings.example.org.nearbyconnections";
    private static final String TAG = "Mobile:";
    Button botonLED;
    TextView textview;
    private Button botonScan;
    private ListView listView;
    private ArrayAdapter<String> adapterListView;
    private Button botonDesconectar;
    private Button botonLedON;
    private Button botonLedOFF;
    int selectedIntem = -1;
//    private Button botonConnectar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textview = (TextView) findViewById(R.id.textView1);
        textview.setText("Pulse SCAN para comenzar");

        listView = (ListView) findViewById(R.id.listViewDevices);
//        botonLED = (Button) findViewById(R.id.buttonLED);
//        botonLED.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                Log.i(TAG, "Boton presionado");
//                startDiscovery();
//                textview.setText("Buscando...");
//            }
//        });
        adapterListView = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                new ArrayList<String>());
        listView.setAdapter(adapterListView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
                selectedIntem = position;
                if (selectedIntem != -1) {
                    String data[]  = adapterListView.getItem(selectedIntem).split("-");
                    String name = adapterListView.getItem(selectedIntem).split("-")[0];
                    String endPoint = adapterListView.getItem(selectedIntem).split("-")[1];
                    connect(endPoint, name);
                }
            }
        });


        botonScan = (Button) findViewById(R.id.buttonScan);
        botonLedON = (Button) findViewById(R.id.buttonLED_ON);
        botonLedON.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String endPointId = adapterListView.getItem(selectedIntem).split("-")[1];
                sendData(endPointId, "SWITCH_ON");
            }
        });
        botonLedOFF = (Button) findViewById(R.id.buttonLED_OFF);
        botonLedOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String endPointId = adapterListView.getItem(selectedIntem).split("-")[1];
                sendData(endPointId, "SWITCH_OFF");
            }
        });
        botonScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "Boton presionado");
                startDiscovery();
                textview.setText("Buscando...");
            }
        });

        botonDesconectar = (Button) findViewById(R.id.buttonDisconnect);

//        botonConnectar = (Button) findViewById(R.id.buttonConnect);
//        botonConnectar.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (selectedIntem != -1) {
//                    String data[]  = adapterListView.getItem(selectedIntem).split("-");
//                    String name = adapterListView.getItem(selectedIntem).split("-")[0];
//                    String endPoint = adapterListView.getItem(selectedIntem).split("-")[1];
//                    connect(endPoint, name);
//                }
//            }
//        });
        botonDesconectar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "Boton desconectar pulsado");

                if (selectedIntem != -1) {
                    String endPoint = adapterListView.getItem(selectedIntem).split("-")[1];
                    disconnect(endPoint);
                }
            }
        });

        // Comprobación de permisos peligrosos
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    private void connect(String endPoint, String name) {

        sendRequestConnection(endPoint, name);
    }

    // Gestión de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permisos concedidos");
                } else {
                    Log.i(TAG, "Permisos denegados");
                    textview.setText("Debe aceptar los permisos para comenzar");
                    botonLED.setEnabled(false);
                }
                return;
            }
        }
    }

    private void startDiscovery() {
        Nearby.getConnectionsClient(this).startDiscovery(SERVICE_ID,
                mEndpointDiscoveryCallback, new DiscoveryOptions(Strategy.P2P_STAR))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unusedResult) {
                        Log.i(TAG, "Estamos en modo descubrimiento!");
                        enableInterfaz(SCAN_FINISHED);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Modo descubrimiento no iniciado.", e);
                    }
                });
    }

    private static final int SCAN_FINISHED = 1;
    private static final int CONNECT_FINISHED = 2;

    private void enableInterfaz(int estado) {
        switch (estado) {
            case SCAN_FINISHED:
                botonScan.setEnabled(false);
//                botonConnectar.setVisibility(View.VISIBLE);
//                botonConnectar.setEnabled(true);
//                botonDesconectar.setVisibility(View.GONE);
//                botonDesconectar.setEnabled(false);
                botonLedON.setEnabled(false);
                botonLedOFF.setEnabled(false);
                listView.setEnabled(true);
                textview.setText("Seleccione un dispositivo para Conectar");
                break;
            case CONNECT_FINISHED:
//                botonScan.setEnabled(false);
//                botonConnectar.setVisibility(View.GONE);
//                botonConnectar.setEnabled(false);
                botonDesconectar.setVisibility(View.VISIBLE);
                botonDesconectar.setEnabled(true);
                botonLedON.setVisibility(View.VISIBLE);
                botonLedON.setEnabled(true);
                botonLedOFF.setVisibility(View.VISIBLE);
                botonLedOFF.setEnabled(true);
                listView.setEnabled(false);
                textview.setText("Conectado! Seleccione una acción a Realizar");
                break;
        }
    }

    private void stopDiscovery() {
        Nearby.getConnectionsClient(this).stopDiscovery();
        Log.i(TAG, "Se ha detenido el modo descubrimiento.");
    }

    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId,
                                            DiscoveredEndpointInfo discoveredEndpointInfo) {

                    String name = discoveredEndpointInfo.getEndpointName();

                    Log.i(TAG, "Descubierto dispositivo con Id: " + endpointId);
                    Log.i(TAG, "Descubierto dispositivo con Nombre: " + name);

                    adapterListView.add(name + "-" + endpointId);
                    adapterListView.notifyDataSetChanged();
                }

                @Override
                public void onEndpointLost(String endpointId) {
                }
            };

    private void sendRequestConnection(String endpointId, String name) {
        stopDiscovery();
        // Iniciamos la conexión con al anunciante "Nearby LED"
        Log.i(TAG, "Conectando...");
        textview.setText("Conectando...");
        Nearby.getConnectionsClient(getApplicationContext())
                .requestConnection(name, endpointId,
                        mConnectionLifecycleCallback)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unusedResult) {
                        Log.i(TAG, "Solicitud lanzada, falta que ambos " +
                                "lados acepten");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error en solicitud de conexión", e);
                        textview.setText("Desconectado");
                        disableInterfaz();
                    }
                });
    }

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(
                        String endpointId, ConnectionInfo connectionInfo) {
                    // Aceptamos la conexión automáticamente en ambos lados.
                    Log.i(TAG, "Aceptando conexión entrante sin autenticación");
                    Nearby.getConnectionsClient(getApplicationContext())
                            .acceptConnection(endpointId, mPayloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId,
                                               ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            Log.i(TAG, "Estamos conectados!");
//                            textview.setText("Conectado");
                            enableInterfaz(CONNECT_FINISHED);
//                            sendData(endpointId, "SWITCH");
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            Log.i(TAG, "Conexión rechazada por uno o ambos lados");
                            textview.setText("Desconectado");
                            disableInterfaz();
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            Log.i(TAG, "Conexión perdida antes de poder ser " +
                                    "aceptada");
                            textview.setText("Desconectado");
                            disableInterfaz();
                            break;
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.i(TAG, "Desconexión del endpoint, no se pueden " +
                            "intercambiar más datos.");
                    textview.setText("Desconectado");
                    disableInterfaz();
                }
            };
    private final PayloadCallback mPayloadCallback = new PayloadCallback() {
        // En este ejemplo, el móvil no recibirá transmisiones de la RP3
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            // Payload recibido
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId,
                                            PayloadTransferUpdate update) {
            // Actualizaciones sobre el proceso de transferencia
        }
    };

    private void sendData(String endpointId, String mensaje) {
        textview.setText("Transfiriendo...");
        Payload data = null;
        try {
            data = Payload.fromBytes(mensaje.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error en la codificación del mensaje.", e);
        }
        Nearby.getConnectionsClient(this).sendPayload(endpointId, data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                textview.setText("Mensaje Enviado...");
                Log.i(TAG, "Mensaje enviado.");
            }
        });

    }


    private void disconnect(String endpointId) {
        Nearby.getConnectionsClient(this)
                .disconnectFromEndpoint(endpointId);
        Log.i(TAG, "Desconectado del endpoint (" + endpointId + ").");
        disableInterfaz();
    }

    private void disableInterfaz() {
        selectedIntem = -1;
        botonScan.setEnabled(true);
//        botonConnectar.setEnabled(false);
//        botonConnectar.setVisibility(View.GONE);
        botonDesconectar.setEnabled(false);
        botonDesconectar.setVisibility(View.GONE);
        botonLedON.setVisibility(View.GONE);
        botonLedON.setEnabled(false);
        botonLedOFF.setVisibility(View.GONE);
        botonLedOFF.setEnabled(false);
        adapterListView.clear();
        listView.setEnabled(false);
        textview.setText("Pulse SCAN para comenzar");
    }
}

