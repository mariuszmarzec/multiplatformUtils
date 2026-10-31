package com.marzec.cache

interface ManyItemsCacheSaver<ID, MODEL> : CacheSaver<List<MODEL>>, CacheByIdSaver<ID, MODEL>
