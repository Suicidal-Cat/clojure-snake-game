SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

--
-- Database: `snakecake`
--
CREATE DATABASE IF NOT EXISTS `snakecake` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci;
USE `snakecake`;

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `GetMatchHistory` (IN `userId` INT)   BEGIN
    SELECT
    	g.id as id,
        -- Result for the given user
        CASE
            WHEN g.WinnerId IS NULL THEN 'Draw'
            WHEN g.WinnerId = userId THEN 'Won'
            ELSE 'Lost'
        END AS result,

        -- Score from games table
        g.Score AS score,
        g.GameMode as mode,

        -- Opponent username or 'guest' if NULL
        CASE
            WHEN g.UserId1 = userId THEN COALESCE(u2.Username, 'guest')
            WHEN g.UserId2 = userId THEN COALESCE(u1.Username, 'guest')
            ELSE 'guest'
        END AS opponent,

        -- Played ago with minute/hour/day granularity
        CASE
            WHEN TIMESTAMPDIFF(MINUTE, g.CreatedAt, NOW()) < 1 THEN '1 minute ago'
            WHEN TIMESTAMPDIFF(MINUTE, g.CreatedAt, NOW()) < 60 THEN 
                CONCAT(TIMESTAMPDIFF(MINUTE, g.CreatedAt, NOW()), ' minutes ago')
            WHEN TIMESTAMPDIFF(HOUR, g.CreatedAt, NOW()) < 24 THEN 
                CONCAT(TIMESTAMPDIFF(HOUR, g.CreatedAt, NOW()), ' hours ago')
            ELSE 
                CONCAT(TIMESTAMPDIFF(DAY, g.CreatedAt, NOW()), ' days ago')
        END AS played_ago

    FROM games g
    JOIN gametypes gt ON g.GameTypeId = gt.Id
    LEFT JOIN users u1 ON g.UserId1 = u1.Id
    LEFT JOIN users u2 ON g.UserId2 = u2.Id

    WHERE gt.Name = 'Multiplayer'
      AND (g.UserId1 = userId OR g.UserId2 = userId)
      AND (g.UserId1 IS NOT NULL OR g.UserId2 IS NOT NULL)

    ORDER BY g.CreatedAt DESC
    LIMIT 20;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `GetPlayerStats` (IN `targetUserId` INT, IN `friends` INT)   BEGIN
    -- Step 1: All valid multiplayer games (no friend filtering here)
    CREATE TEMPORARY TABLE valid_games AS
    SELECT g.*
    FROM games g
    JOIN gametypes gt ON g.GameTypeId = gt.Id
    WHERE gt.Name = 'Multiplayer'
      AND (g.UserId1 IS NOT NULL OR g.UserId2 IS NOT NULL)
      AND g.WinnerId IS NOT NULL;

    -- Step 2: Compute stats per user from ALL valid_games
    CREATE TEMPORARY TABLE user_stats AS
    SELECT
        u.Id AS UserId,
        u.Username,
        COUNT(g.Id) AS GamesPlayed,
        SUM(CASE WHEN g.WinnerId = u.Id THEN 1 ELSE 0 END) AS GamesWon,
        SUM(CASE WHEN (g.UserId1 = u.Id OR g.UserId2 = u.Id) AND g.WinnerId != u.Id THEN 1 ELSE 0 END) AS GamesLost,
        ROUND(100 * SUM(CASE WHEN g.WinnerId = u.Id THEN 1 ELSE 0 END) / COUNT(g.Id), 2) AS WinPercentage
    FROM users u
    JOIN valid_games g ON u.Id = g.UserId1 OR u.Id = g.UserId2
    GROUP BY u.Id;

    -- Step 3: Global ranking (based on all games)
    CREATE TEMPORARY TABLE ranked_users AS
    SELECT
        *,
        RANK() OVER (ORDER BY GamesWon DESC, WinPercentage DESC) AS RankPos
    FROM user_stats;

    -- Step 4: Build friend list for filtering
    CREATE TEMPORARY TABLE friends_of_target AS
    SELECT DISTINCT
        CASE WHEN UserId1 = targetUserId THEN UserId2 ELSE UserId1 END AS FriendId
    FROM friendships
    WHERE (UserId1 = targetUserId OR UserId2 = targetUserId)
      AND Status = 'Accepted';

    -- Step 5: Return results
    (
        -- Top10: global if friends=0, else only friends + target user
        SELECT
            ru.UserId, ru.Username, ru.GamesPlayed, ru.GamesWon, ru.GamesLost, ru.WinPercentage, ru.RankPos,
            'Top10' AS ResultType
        FROM ranked_users ru
        WHERE (friends = 0)
           OR (ru.UserId = targetUserId)
           OR (ru.UserId IN (SELECT FriendId FROM friends_of_target))
        ORDER BY ru.RankPos
        LIMIT 10
    )
    UNION ALL
    (
        -- Always include the target user as 'Target'
        SELECT
            ru.UserId, ru.Username, ru.GamesPlayed, ru.GamesWon, ru.GamesLost, ru.WinPercentage, ru.RankPos,
            'Target' AS ResultType
        FROM ranked_users ru
        WHERE ru.UserId = targetUserId
    );

    -- Cleanup
    DROP TEMPORARY TABLE IF EXISTS valid_games;
    DROP TEMPORARY TABLE IF EXISTS user_stats;
    DROP TEMPORARY TABLE IF EXISTS ranked_users;
    DROP TEMPORARY TABLE IF EXISTS friends_of_target;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `cakeingredients`
--

CREATE TABLE `cakeingredients` (
  `Id` int(11) NOT NULL,
  `CakeId` int(11) NOT NULL,
  `IngredientId` int(11) NOT NULL,
  `Amount` int(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `cakeingredients`
--

INSERT INTO `cakeingredients` (`Id`, `CakeId`, `IngredientId`, `Amount`) VALUES
(1, 4, 4, 6),
(2, 4, 8, 2),
(3, 4, 10, 2),
(4, 4, 2, 2),
(5, 4, 1, 2),
(6, 5, 6, 6),
(7, 5, 1, 2),
(8, 5, 3, 2),
(9, 5, 9, 2),
(10, 5, 2, 2),
(11, 6, 5, 6),
(12, 6, 7, 2),
(13, 6, 3, 2),
(14, 6, 10, 2),
(15, 6, 2, 2);

-- --------------------------------------------------------

--
-- Table structure for table `cakes`
--

CREATE TABLE `cakes` (
  `Id` int(11) NOT NULL,
  `Name` varchar(20) NOT NULL,
  `ImageName` varchar(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `cakes`
--

INSERT INTO `cakes` (`Id`, `Name`, `ImageName`) VALUES
(4, 'Strawberry Cake', 'strawberryCake.png'),
(5, 'Apple Cake', 'appleCake.png'),
(6, 'Cherry Cake', 'cherryCake.png');

-- --------------------------------------------------------

--
-- Table structure for table `friendships`
--

CREATE TABLE `friendships` (
  `Id` int(11) NOT NULL,
  `UserId1` bigint(11) NOT NULL,
  `UserId2` bigint(11) NOT NULL,
  `Status` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `games`
--

CREATE TABLE `games` (
  `Id` bigint(20) NOT NULL,
  `UserId1` bigint(20) DEFAULT NULL,
  `UserId2` bigint(20) DEFAULT NULL,
  `Score` varchar(50) NOT NULL,
  `WinnerId` bigint(20) DEFAULT NULL,
  `GameTypeId` int(11) NOT NULL,
  `GameMode` varchar(30) DEFAULT NULL,
  `CreatedAt` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- --------------------------------------------------------

--
-- Table structure for table `gametypes`
--

CREATE TABLE `gametypes` (
  `Id` int(11) NOT NULL,
  `Name` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `gametypes`
--

INSERT INTO `gametypes` (`Id`, `Name`) VALUES
(1, 'Multiplayer'),
(2, 'Singleplayer');

-- --------------------------------------------------------

--
-- Table structure for table `ingredients`
--

CREATE TABLE `ingredients` (
  `Id` int(11) NOT NULL,
  `Name` varchar(20) NOT NULL,
  `ImageName` varchar(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Dumping data for table `ingredients`
--

INSERT INTO `ingredients` (`Id`, `Name`, `ImageName`) VALUES
(1, 'Milk', 'milk.png'),
(2, 'Butter', 'butter.png'),
(3, 'Honey', 'honey.png'),
(4, 'Strawberry', 'strawberry.png'),
(5, 'Cherry', 'cherry.png'),
(6, 'Apple', 'apple.png'),
(7, 'Grape', 'grape.png'),
(8, 'Lemon', 'lemon.png'),
(9, 'Banana', 'banana.png'),
(10, 'Floury', 'floury.png');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `Id` bigint(20) NOT NULL,
  `Email` varchar(255) NOT NULL,
  `Username` varchar(50) NOT NULL,
  `Password` varchar(255) NOT NULL,
  `IsActive` int(11) NOT NULL,
  `CreatedAt` datetime DEFAULT NULL,
  `LastLogin` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `cakeingredients`
--
ALTER TABLE `cakeingredients`
  ADD PRIMARY KEY (`Id`),
  ADD KEY `CakeId` (`CakeId`),
  ADD KEY `IngredientId` (`IngredientId`);

--
-- Indexes for table `cakes`
--
ALTER TABLE `cakes`
  ADD PRIMARY KEY (`Id`);

--
-- Indexes for table `friendships`
--
ALTER TABLE `friendships`
  ADD PRIMARY KEY (`Id`),
  ADD KEY `UserId1` (`UserId1`),
  ADD KEY `UserId2` (`UserId2`);

--
-- Indexes for table `games`
--
ALTER TABLE `games`
  ADD PRIMARY KEY (`Id`),
  ADD KEY `UserId1` (`UserId1`,`UserId2`,`GameTypeId`),
  ADD KEY `GameTypeId` (`GameTypeId`),
  ADD KEY `UserId2` (`UserId2`),
  ADD KEY `WinnerId` (`WinnerId`);

--
-- Indexes for table `gametypes`
--
ALTER TABLE `gametypes`
  ADD PRIMARY KEY (`Id`),
  ADD UNIQUE KEY `Name` (`Name`);

--
-- Indexes for table `ingredients`
--
ALTER TABLE `ingredients`
  ADD PRIMARY KEY (`Id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`Id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `cakeingredients`
--
ALTER TABLE `cakeingredients`
  MODIFY `Id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `cakes`
--
ALTER TABLE `cakes`
  MODIFY `Id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `friendships`
--
ALTER TABLE `friendships`
  MODIFY `Id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `games`
--
ALTER TABLE `games`
  MODIFY `Id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `gametypes`
--
ALTER TABLE `gametypes`
  MODIFY `Id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `ingredients`
--
ALTER TABLE `ingredients`
  MODIFY `Id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `Id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `cakeingredients`
--
ALTER TABLE `cakeingredients`
  ADD CONSTRAINT `cakeingredients_ibfk_1` FOREIGN KEY (`CakeId`) REFERENCES `cakes` (`Id`),
  ADD CONSTRAINT `cakeingredients_ibfk_2` FOREIGN KEY (`IngredientId`) REFERENCES `ingredients` (`Id`);

--
-- Constraints for table `friendships`
--
ALTER TABLE `friendships`
  ADD CONSTRAINT `friendships_ibfk_1` FOREIGN KEY (`UserId1`) REFERENCES `users` (`Id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `friendships_ibfk_2` FOREIGN KEY (`UserId2`) REFERENCES `users` (`Id`) ON UPDATE CASCADE;

--
-- Constraints for table `games`
--
ALTER TABLE `games`
  ADD CONSTRAINT `games_ibfk_1` FOREIGN KEY (`GameTypeId`) REFERENCES `gametypes` (`Id`),
  ADD CONSTRAINT `games_ibfk_2` FOREIGN KEY (`UserId1`) REFERENCES `users` (`Id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `games_ibfk_3` FOREIGN KEY (`UserId2`) REFERENCES `users` (`Id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `games_ibfk_4` FOREIGN KEY (`WinnerId`) REFERENCES `users` (`Id`);
COMMIT;

