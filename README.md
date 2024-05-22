# ZeroCollateral API

## Background
The "ZeroCollateral" api is for ...

## API Handlers
The available handlers are for managing audit operations and searches over the data and images catalog

| OPERATION API        | Method | Auth  |                                                                                                                                     Description |
|----------------------|--------|-------|------------------------------------------------------------------------------------------------------------------------------------------------:|
| /api/coins/ | GET    | Basic | Retrieves details of coins for which there is at least one active market |
| /api/markets/ | GET    | Basic | List of markets where a specific coin is traded along with all the details |
| /api/collateral/ | GET    | Basic | Retrieve the details about the collaterals available in the specified market |

### Local development

The local environment can be started quickly by importing the launch 
configuration [ZeroCollateral](support/config/local/ZeroCollateral.xml) in the IntelliJ IDE.

For the application to be running it is available a local stack environment with all the system, 
you can start it with docker compose of the following directory docker/compose/local-dev
