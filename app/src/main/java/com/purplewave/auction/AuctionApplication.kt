package com.purplewave.auction

import android.app.Application

class AuctionApplication : Application() {
    // ServiceLocator is lazy — no explicit init needed here.
    // This class exists as a hook for future DI framework setup.
}
