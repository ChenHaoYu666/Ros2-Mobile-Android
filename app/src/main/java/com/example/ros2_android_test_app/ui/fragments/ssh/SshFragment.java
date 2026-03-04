package com.example.ros2_android_test_app.ui.fragments.ssh;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.ros2_android_test_app.R;
import com.example.ros2_android_test_app.databinding.FragmentSshBinding;
import com.example.ros2_android_test_app.viewmodel.SshViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SshFragment extends Fragment implements TextView.OnEditorActionListener {

    public static final String TAG = SshFragment.class.getCanonicalName();

    private static final String PREF_NAME = "ssh_terminal_prefs";
    private static final String KEY_HISTORY = "command_history";
    private static final int MAX_HISTORY = 50;

    private SshViewModel mViewModel;
    private FragmentSshBinding binding;
    private RecyclerView recyclerView;
    private SshRecyclerViewAdapter mAdapter;
    private AutoCompleteTextView terminalEditText;
    private Button connectButton;
    private FloatingActionButton sendButton;
    private FloatingActionButton abortButton;
    private boolean connected;

    private ArrayAdapter<String> historyAdapter;
    private final List<String> commandHistory = new ArrayList<>();
    private int historyCursor = -1;

    public static SshFragment newInstance() {
        return new SshFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSshBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setConnectionData();
        saveHistory();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mAdapter = new SshRecyclerViewAdapter();

        recyclerView = view.findViewById(R.id.outputRV);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);

        terminalEditText = view.findViewById(R.id.terminal_editText);
        connectButton = view.findViewById(R.id.sshConnectButton);
        sendButton = view.findViewById(R.id.sshSendButton);
        abortButton = view.findViewById(R.id.sshAbortButton);

        String[] autocompletion = getResources().getStringArray(R.array.ssh_autocmpletion);
        Arrays.sort(autocompletion);

        loadHistory();
        for (String preset : autocompletion) {
            if (!commandHistory.contains(preset)) {
                commandHistory.add(preset);
            }
        }

        historyAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, commandHistory);
        terminalEditText.setAdapter(historyAdapter);
        terminalEditText.setThreshold(1);

        terminalEditText.setOnEditorActionListener((v1, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                executeCommandFromInput();
                return true;
            }
            return false;
        });

        terminalEditText.setOnKeyListener((v12, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                browseHistoryUp();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                browseHistoryDown();
                return true;
            }

            return false;
        });

        terminalEditText.setOnClickListener(v13 -> {
            if (terminalEditText.getText() == null || terminalEditText.getText().toString().isEmpty()) {
                terminalEditText.showDropDown();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(SshViewModel.class);

        mViewModel.getSSH().observe(getViewLifecycleOwner(), ssh -> {
            if (ssh == null) return;
            binding.ipAddressEditText.setText(ssh.ip);
            binding.portEditText.setText(Integer.toString(ssh.port));
            binding.usernameEditText.setText(ssh.username);
            binding.passwordEditText.setText(ssh.password);
        });

        connectButton.setOnClickListener(v -> {
            if (connected) {
                mViewModel.stopSsh();
            } else {
                setConnectionData();
                connectSsh();
            }
        });

        sendButton.setOnClickListener(v -> executeCommandFromInput());

        abortButton.setOnClickListener(v -> mViewModel.abortAction());

        mViewModel.getOutputData().observe(this.getViewLifecycleOwner(), s -> {
            mAdapter.addItem(s);
            recyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
        });

        mViewModel.isConnected().observe(this.getViewLifecycleOwner(), connectionFlag -> {
            connected = connectionFlag;
            if (connectionFlag) {
                connectButton.setText("Disconnect");
            } else {
                connected = false;
                connectButton.setText("Connect");
            }
        });

        binding.ipAddressEditText.setOnEditorActionListener(this);
        binding.portEditText.setOnEditorActionListener(this);
        binding.usernameEditText.setOnEditorActionListener(this);
        binding.passwordEditText.setOnEditorActionListener(this);
    }

    private void executeCommandFromInput() {
        if (terminalEditText == null) return;

        final String message = terminalEditText.getText() == null ? "" : terminalEditText.getText().toString().trim();
        if (message.isEmpty()) return;

        addToHistory(message);
        mViewModel.sendViaSSH(message);
        terminalEditText.setText("");
        historyCursor = -1;
        hideSoftKeyboard();
    }

    private void browseHistoryUp() {
        if (commandHistory.isEmpty()) return;

        if (historyCursor < commandHistory.size() - 1) {
            historyCursor++;
        }
        applyHistoryCursorText();
    }

    private void browseHistoryDown() {
        if (commandHistory.isEmpty()) return;

        if (historyCursor > 0) {
            historyCursor--;
            applyHistoryCursorText();
            return;
        }

        historyCursor = -1;
        terminalEditText.setText("");
        terminalEditText.setSelection(0);
    }

    private void applyHistoryCursorText() {
        if (historyCursor < 0 || historyCursor >= commandHistory.size()) return;
        String cmd = commandHistory.get(historyCursor);
        terminalEditText.setText(cmd);
        terminalEditText.setSelection(cmd.length());
    }

    private void addToHistory(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) return;

        commandHistory.remove(cmd);
        commandHistory.add(0, cmd);

        while (commandHistory.size() > MAX_HISTORY) {
            commandHistory.remove(commandHistory.size() - 1);
        }

        historyCursor = -1;

        if (historyAdapter != null) {
            historyAdapter.clear();
            historyAdapter.addAll(commandHistory);
            historyAdapter.notifyDataSetChanged();
        }

        saveHistory();
    }

    private void loadHistory() {
        commandHistory.clear();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_HISTORY, "");
        if (raw == null || raw.trim().isEmpty()) return;

        String[] arr = raw.split("\\n");
        for (String item : arr) {
            String trimmed = item == null ? "" : item.trim();
            if (!trimmed.isEmpty() && !commandHistory.contains(trimmed)) {
                commandHistory.add(trimmed);
            }
        }
    }

    private void saveHistory() {
        if (getContext() == null) return;

        StringBuilder sb = new StringBuilder();
        for (String cmd : commandHistory) {
            if (cmd == null || cmd.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(cmd.replace('\n', ' '));
        }

        requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HISTORY, sb.toString())
                .apply();
    }

    private void connectSsh() {
        mViewModel.connectViaSSH();
    }

    private void hideSoftKeyboard() {
        if (getActivity() == null || getView() == null) return;
        final InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            setConnectionData();
            view.clearFocus();
            hideSoftKeyboard();
            return true;
        }
        return false;
    }

    public void setConnectionData() {
        Editable sshIp = binding.ipAddressEditText.getText();
        if (sshIp != null) {
            mViewModel.setSshIp(sshIp.toString());
        }

        Editable sshPort = binding.portEditText.getText();
        if (sshPort != null) {
            mViewModel.setSshPort(sshPort.toString());
        }

        Editable sshPassword = binding.passwordEditText.getText();
        if (sshPassword != null) {
            mViewModel.setSshPassword(sshPassword.toString());
        }

        Editable sshUsername = binding.usernameEditText.getText();
        if (sshUsername != null) {
            mViewModel.setSshUsername(sshUsername.toString());
        }
    }
}
