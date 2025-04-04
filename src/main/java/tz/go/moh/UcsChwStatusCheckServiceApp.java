package tz.go.moh;

import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.server.Route;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletionStage;


/**
 * Main class for the UCS CHW Status Check Service application.
 */
public class UcsChwStatusCheckServiceApp {
    /**
     * Starts the HTTP server.
     *
     * @param route  The HTTP routes to handle requests.
     * @param system The actor system.
     */
    static void startHttpServer(Route route, ActorSystem<?> system) {
        CompletionStage<ServerBinding> futureBinding =
            Http.get(system).newServerAt(system.settings().config().getString("chw-status-check-service.service-host"), system.settings().config().getInt("chw-status-check-service.service-port")).bind(route);

        futureBinding.whenComplete((binding, exception) -> {
            if (binding != null) {
                InetSocketAddress address = binding.localAddress();
                system.log().info("Server online at http://{}:{}/",
                    address.getHostString(),
                    address.getPort());
            } else {
                system.log().error("Failed to bind HTTP endpoint, terminating system", exception);
                system.terminate();
            }
        });
    }

    /**
     * Main method to start the application.
     *
     * @param args Command line arguments.
     * @throws Exception If an error occurs during startup.
     */
    public static void main(String[] args) throws Exception {
        //#server-bootstrapping
        Behavior<NotUsed> rootBehavior = Behaviors.setup(context -> {
            ActorRef<UcsChwStatusCheckRegistry.Command> userRegistryActor =
                context.spawn(UcsChwStatusCheckRegistry.create(), "UcsChwStatusCheck");

            UcsChwStatusCheckRoutes ucsChwStatusCheckRoutes = new UcsChwStatusCheckRoutes(context.getSystem(), userRegistryActor);
            startHttpServer(ucsChwStatusCheckRoutes.checkChwMonthlyStatusRoutes(), context.getSystem());

            return Behaviors.empty();
        });

        // boot up server using the route as defined below
        ActorSystem.create(rootBehavior, "UcsChwStatusCheckServiceServer");
        //#server-bootstrapping
    }

}
