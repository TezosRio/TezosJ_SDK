package milfont.com.tezosj_android.data;

import org.jetbrains.annotations.NotNull;

import network.pocket.core.Pocket;

class PocketProvider extends Pocket {
    public PocketProvider(@NotNull String devID, @NotNull String network,
                          @NotNull String[] netIds, int maxNodes, int requestTimeOut) {
        super(devID, network, netIds, maxNodes, requestTimeOut);
    }
}
