package uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint;

import java.util.HashSet;
import java.util.Set;

public final class SpawnpointFlags {
    public boolean ifHas;
    public boolean ifMissing;
    public boolean onlyIfIncorrect;
    public boolean requireValidBed;
    public boolean dryRun;

    public static SpawnpointFlags parse(String[] raw) {
        SpawnpointFlags f = new SpawnpointFlags();
        for (String t : raw) {
            if (t == null) continue;
            String s = t.trim().toLowerCase();
            switch (s) {
                case "--if-has" -> f.ifHas = true;
                case "--if-missing" -> f.ifMissing = true;
                case "--only-if-incorrect" -> f.onlyIfIncorrect = true;
                case "--require-valid-bed" -> f.requireValidBed = true;
                case "--dry-run" -> f.dryRun = true;
                default -> { /* ignore */ }
            }
        }
        return f;
    }

    public void validateMutual() {
        if (ifHas && ifMissing) {
            throw new IllegalArgumentException("Flags --if-has and --if-missing cannot be used together");
        }
    }

    public static String[] stripFlags(String[] raw) {
        if (raw == null || raw.length == 0) return new String[0];
        Set<Integer> idx = new HashSet<>();
        for (int i = 0; i < raw.length; i++) {
            String s = raw[i];
            if (s != null && s.startsWith("--")) idx.add(i);
        }
        String[] out = new String[raw.length - idx.size()];
        int k = 0;
        for (int i = 0; i < raw.length; i++) {
            if (idx.contains(i)) continue;
            out[k++] = raw[i];
        }
        return out;
    }
}