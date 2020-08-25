
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class ThrottlerSimulation extends Simulation {

	val httpProtocol = http
		.baseUrl("http://localhost:8080")

	val users = scenario("Users").exec(Hi.withToken)
	val guests = scenario("Users").exec(Hi.withoutToken)

	setUp(users.inject(rampUsers(100) during (10 second))).protocols(httpProtocol)
}

object Hi {
	val withoutToken = repeat(10, "n"){
		exec(http("request_0").get("/hi"))
		.pause(1 second)
	}
	val withToken = repeat(10, "n"){
		exec(http("request_0").get("/hi?token=t_${n}"))
		.pause(1 second)
	}
}