package com.mitsest.endlessrecyclerexample.dagger

import android.app.Application
import com.mitsest.endlessrecyclerexample.EndlessRecyclerViewExampleApplication
import com.mitsest.endlessrecyclerexample.MainActivity
import com.mitsest.endlessrecyclerexample.PersonApiClient
import com.mitsest.endlessrecyclerexample.dagger.modules.ApplicationModule
import dagger.*
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Subcomponent
interface MainActivityInjector: AndroidInjector<MainActivity> {
    @Subcomponent.Builder
    abstract class Builder: AndroidInjector.Builder<MainActivity>()
}

@Module(
    subcomponents = [
        MainActivityInjector::class
    ]
)
abstract class EndlessRecyclerViewExampleInjectorModule {
    @Binds
    @IntoMap
    @ClassKey(MainActivity::class)
    abstract fun MainActivityActivityInjector(builder: MainActivityInjector.Builder): AndroidInjector.Factory<*>
}

@Singleton
@Component(modules = [EndlessRecyclerViewExampleInjectorModule::class, ApplicationModule::class, AndroidInjectionModule::class])
interface EndlessRecyclerViewExampleApplicationComponent {
    fun inject(a: EndlessRecyclerViewExampleApplication)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder
        fun build(): EndlessRecyclerViewExampleApplicationComponent
    }
}
