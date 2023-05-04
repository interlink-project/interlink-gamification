package eu.fbk.interlink.gamification.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import eu.fbk.interlink.gamification.component.GameComponent;
import eu.fbk.interlink.gamification.component.GamificationEngineFacadeComponent;
import eu.fbk.interlink.gamification.domain.InterlinkGame;
import eu.fbk.interlink.gamification.domain.InterlinkGameTemplate;
import eu.fbk.interlink.gamification.domain.InterlinkPlayer;
import eu.fbk.interlink.gamification.domain.InterlinkTask;
import eu.fbk.interlink.gamification.domain.PlayerScore;
import eu.fbk.interlink.gamification.domain.PlayerStateDTO;
import eu.fbk.interlink.gamification.repository.InterLinkerRepository;
import eu.fbk.interlink.gamification.util.ControllerUtils;
import eu.trentorise.game.model.GameStatistics;
import eu.trentorise.game.model.PlayerState;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping("/interlink")
@Profile({ "no-sec", "sec", "default" })
public class GameRestController {

	private static final Logger logger = LogManager.getLogger(GameRestController.class);

	@Autowired
	private GameComponent gameComponent;

	@Autowired
	private GamificationEngineFacadeComponent gamificationComponent;

	@Autowired
	private InterLinkerRepository interlinkRepo;

	/**
	 * Return all the games present in the DB
	 * 
	 * @return List of Game
	 */
	@GetMapping(value = "/game")
	public List<InterlinkGame> getAllGames() {
		return gameComponent.findAll();
	}

	/**
	 * Return the game with a specific gameId
	 * 
	 * @param gameId Game Id
	 * @return Game
	 */
	@GetMapping(value = "/game/{gameId}")
	public Optional<InterlinkGame> getGame(@PathVariable String gameId) {
		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		return game;
	}
	
	/**
	 * Return the game with a specific gameId
	 * 
	 * @param gameId Game Id
	 * @return Game
	 */
	@DeleteMapping(value = "/game/{gameId}")
	public void deleteGame(@PathVariable String gameId) {
		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (game.isEmpty()) {
			return;
		}

		String coreGameId = ControllerUtils.getGameId(game.get().getProcessId(), game.get().getName());
		gameComponent.deleteGameById(gameId);
		interlinkRepo.deleteGameRule(coreGameId);
	}

	/**
	 * Return all the games present in the DB related to a process
	 * 
	 * @param processId Process Id
	 * @return List of Game
	 */
	@GetMapping(value = "/game/processId/{processId}")
	public List<InterlinkGame> getGamesByProcessId(@PathVariable String processId) {
		return gameComponent.findByProcessId(processId);
	}

	/**
	 * Return the game with a specific gameId
	 * 
	 * @param processId Process ID
	 * @param name      Game name
	 * @return Game
	 */
	@GetMapping(value = "/game/processId/{processId}/name/{name}")
	public Optional<InterlinkGame> getGame(@PathVariable String processId, @PathVariable String name) {
		return gameComponent.findByProcessIdAndName(processId, name);
	}

	/**
	 * Create a game from template
	 * 
	 * @param game
	 * @return Message
	 */
	@PostMapping(value = "/game/processId/{processId}")
	public ResponseEntity<?> newGameFromTemplate(@PathVariable String processId,
			@RequestBody InterlinkGameTemplate template) {

		processId = ControllerUtils.decodePathVariable(processId);

		if ((gameComponent.findByProcessIdAndName(processId, template.getName()).isPresent())) {
			return new ResponseEntity(
					"Game " + template.getName() + " for process " + processId + " is already present",
					HttpStatus.PRECONDITION_FAILED);
		}

		// instantiate the game in fbk gamification engine
		if (template.getFilename() != null && !template.getFilename().isEmpty()) {
			try {
				gamificationComponent.instanceAndConfigureGame(processId, template);
			} catch (Exception e) {
				logger.error("Error in game creation from file " + template.getFilename() + " ", e);
				return new ResponseEntity("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else {
			logger.error("Missing filename inside template");
			return new ResponseEntity("Missing filename inside template", HttpStatus.BAD_REQUEST);
		}

		// instantiate the game in interlink gamification engine
		InterlinkGame newGame = InterlinkGame.of(template);
		newGame.setProcessId(processId);
		newGame = gameComponent.saveOrUpdateGame(newGame);
		logger.info("New game " + newGame.getName() + " for processId " + newGame.getProcessId() + "has been created");

		return new ResponseEntity(newGame, HttpStatus.OK);
	}

	/**
	 * Update a game
	 * 
	 * @param game
	 * @return Message
	 */
	@PutMapping(value = "/game")
	public ResponseEntity<?> updateGame(@RequestBody InterlinkGame game) {

		if ((game.getId() == null) || (gameComponent.findById(game.getId()).isEmpty())) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}

		if (!gameComponent.findById(game.getId()).get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}

		gameComponent.saveOrUpdateGame(game);
		return new ResponseEntity("Game updated successfully", HttpStatus.OK);
	}

	/**
	 * Return a game task
	 * 
	 * @param gameId
	 * @param taskId
	 * @return Task
	 */
	@GetMapping(value = "/game/{gameId}/task/{taskId}")
	public Optional<InterlinkTask> getTask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId) {
		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);
		Optional<InterlinkTask> task = null;
		if (!game.isPresent())
			return Optional.empty();

		for (InterlinkTask element : game.get().getTaskList()) {
			if (element.getId().equals(taskId)) {
				task = Optional.of(element);
				break;
			}
		}
		return task;
	}

	/**
	 * Update a game task
	 * 
	 * @param gameId
	 * @param taskId
	 * @return Message
	 */
	@PutMapping(value = "/game/{gameId}/task")
	public ResponseEntity<?> updateTask(@PathVariable String gameId, @RequestBody InterlinkTask task) {

		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (!game.isPresent()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}

		if (!game.get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}

		InterlinkTask savedTask = game.get().getTaskList().stream().filter(t -> task.getId().equals(t.getId()))
				.findAny().orElse(null);

		if (savedTask != null) {
			
			if (savedTask.isCompleted()) {
				return new ResponseEntity("Task is already completed", HttpStatus.PRECONDITION_FAILED);
			}
			
			savedTask.setCompleted(task.isCompleted());
			savedTask.setDevelopment(task.getDevelopment());
			savedTask.setExploitation(task.getExploitation());
			savedTask.setManagement(task.getManagement());
			savedTask.setSubtaskList(task.getSubtaskList());

			for (InterlinkPlayer updated : task.getPlayers()) {
				InterlinkPlayer savedPlayer = savedTask.getPlayers().stream()
						.filter(p -> p.getId().equals(updated.getId())).findAny().orElse(null);
				if (savedPlayer != null) {
					savedPlayer.setName(updated.getName());
					savedPlayer.setDevelopment(updated.getDevelopment());
					savedPlayer.setExploitation(updated.getExploitation());
					savedPlayer.setManagement(updated.getManagement());
				} else {
					savedTask.getPlayers().add(updated);
				}
			}
			InterlinkGame result = gameComponent.saveOrUpdateGame(game.get());
			return new ResponseEntity(result, HttpStatus.OK);
		} else {
			return new ResponseEntity("Task " + task.getId() + " not present inside game",
					HttpStatus.PRECONDITION_FAILED);
		}
	}

	/**
	 * Claim a task
	 * 
	 * @param gameId
	 * @param taskId
	 * @return Message
	 */
	@PutMapping(value = "/game/{gameId}/task/{taskId}/claim")
	public ResponseEntity<?> claimTask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId, @RequestBody InterlinkPlayer player) {

		gameId = ControllerUtils.decodePathVariable(gameId);
		String idTask = ControllerUtils.decodePathVariable(taskId);

		Optional<InterlinkGame> game = gameComponent.findById(gameId);
		if (!game.isPresent()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}

		if (!game.get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}

		InterlinkTask task = game.get().getTaskList().stream().filter(t -> idTask.equals(t.getId())).findAny()
				.orElse(null);

		if (task != null) {
			
			if (task.isCompleted()) {
				return new ResponseEntity("Task is already completed ",	HttpStatus.PRECONDITION_FAILED);
			}
			
			boolean update = false;
			InterlinkPlayer isPresent = task.getPlayers().stream()
					.filter(findPlayer -> player.getId().equals(findPlayer.getId())).findAny().orElse(null);
			if (isPresent == null) {
				task.addPlayer(player);
				update = true;
			} else {
				return new ResponseEntity("Player " + player.getId() + " already present inside task " + taskId,
						HttpStatus.PRECONDITION_FAILED);
			}
			if (update) {
				InterlinkGame result = gameComponent.saveOrUpdateGame(game.get());
				return new ResponseEntity(result, HttpStatus.OK);
			}
		} else {
			return new ResponseEntity("Task " + taskId + " not present inside game", HttpStatus.PRECONDITION_FAILED);
		}

		return new ResponseEntity("Player " + player.getId() + " has non been added to subtask " + taskId,
				HttpStatus.PRECONDITION_FAILED);
	}
	
	/**
	 * Claim a task
	 * 
	 * @param gameId
	 * @param taskId
	 * @return Message
	 */
	@DeleteMapping(value = "/game/{gameId}/task/{taskId}/removePlayer")
	public ResponseEntity<?> removeFromTask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId, @RequestBody InterlinkPlayer player) {

		gameId = ControllerUtils.decodePathVariable(gameId);
		String idTask = ControllerUtils.decodePathVariable(taskId);

		Optional<InterlinkGame> game = gameComponent.findById(gameId);
		if (!game.isPresent()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}

		if (!game.get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}

		InterlinkTask task = game.get().getTaskList().stream().filter(t -> idTask.equals(t.getId())).findAny()
				.orElse(null);

		if (task != null) {
			
			if (task.isCompleted()) {
				return new ResponseEntity("Task already completed", HttpStatus.PRECONDITION_FAILED);
			}
			
			boolean update = false;
			InterlinkPlayer isPresent = task.getPlayers().stream()
					.filter(findPlayer -> player.getId().equals(findPlayer.getId())).findAny().orElse(null);
			if (isPresent != null) {
				task.removePlayer(player);
				update = true;
			} else {
				return new ResponseEntity("Player " + player.getId() + " not present inside task " + taskId,
						HttpStatus.PRECONDITION_FAILED);
			}
			if (update) {
				InterlinkGame result = gameComponent.saveOrUpdateGame(game.get());
				// remove from player state
				this.gamificationComponent.deletePlayerState(game.get().getProcessId(), game.get().getName(), player.getId());
				return new ResponseEntity(result, HttpStatus.OK);
			}
		} else {
			return new ResponseEntity("Task " + taskId + " not present inside game", HttpStatus.PRECONDITION_FAILED);
		}

		return new ResponseEntity("Player " + player.getId() + " has non been removed to task " + taskId,
				HttpStatus.PRECONDITION_FAILED);
	}

	/**
	 * Complete a game task
	 * 
	 * @param gameId
	 * @param taskId
	 * @return Message
	 */
	@PutMapping(value = "/game/{gameId}/task/{taskId}/complete")
	public ResponseEntity<?> completeTask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId) {
		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);
		if (!game.isPresent()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}
		if (!game.get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}
		InterlinkTask savedTask = game.get().getTaskList().stream().filter(t -> taskId.equals(t.getId())).findAny()
				.orElse(null);
		if (savedTask != null) {
			if (savedTask.isCompleted()) {
				return new ResponseEntity("Task already completed ", HttpStatus.PRECONDITION_FAILED);
			}
			// update subtask player points
			for (InterlinkTask subtask : savedTask.getSubtaskList()) {
				if (!subtask.isCompleted()) {
					return new ResponseEntity("You have to complete  subtask " + subtask.getId()
							+ " before to complete the task " + savedTask.getId(), HttpStatus.PRECONDITION_FAILED);
				}
			}
			// update task the player points
			for (InterlinkPlayer player : savedTask.getPlayers()) {
				gamificationComponent.triggerAction(game.get().getProcessId(), game.get().getName(),
						"update_player_points", player, savedTask);
			}
			savedTask.setCompleted(true);
			InterlinkGame result = gameComponent.saveOrUpdateGame(game.get());
			return new ResponseEntity(result, HttpStatus.OK);
		} else {
			return new ResponseEntity("Task " + taskId + " is not present in game " + gameId,
					HttpStatus.PRECONDITION_FAILED);
		}
	}
	
	/**
	 * Revert a game task with scores
	 * 
	 * @param gameId
	 * @param taskId
	 * @return Message
	 */
	@PutMapping(value = "/game/{gameId}/task/{taskId}/revert")
	public ResponseEntity<?> revertTask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId) {
		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);
		if (!game.isPresent()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}
		if (!game.get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}
		InterlinkTask savedTask = game.get().getTaskList().stream().filter(t -> taskId.equals(t.getId())).findAny()
				.orElse(null);
		if (savedTask != null) {
			if (!savedTask.isCompleted()) {
				return new ResponseEntity("Task " + savedTask.getId() + "must be completed first.",
						HttpStatus.PRECONDITION_FAILED);
			}
			// revert task with player points
			for (InterlinkPlayer player : savedTask.getPlayers()) {
				gamificationComponent.triggerAction(game.get().getProcessId(), game.get().getName(),
						"revert_player_points", player, savedTask);
			}
			savedTask.setCompleted(false);
			InterlinkGame result = gameComponent.saveOrUpdateGame(game.get());
			return new ResponseEntity(result, HttpStatus.OK);
		} else {
			return new ResponseEntity("Task " + taskId + " is not present in game " + gameId,
					HttpStatus.PRECONDITION_FAILED);
		}
	}

	/**
	 * Create a subtask
	 * 
	 * @param gameId
	 * @param taskId
	 * @param sutaskId
	 * @return Message
	 */
	@PostMapping(value = "/game/{gameId}/task/{taskId}/subtask")
	public ResponseEntity<?> newSubtask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId, @RequestBody InterlinkTask subtask) {

		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (!game.isPresent()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}

		if (!game.get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}

		if (ControllerUtils.isEmpty(subtask.getId())) {
			return new ResponseEntity("subTask Id cannot be null", HttpStatus.BAD_REQUEST);
		}

		boolean update = false;

		for (InterlinkTask element : game.get().getTaskList()) {
			if (element.getId().equals(taskId)) {
				for (InterlinkTask subElement : element.getSubtaskList()) {
					if (subElement.getId().equals(subtask.getId()))
						return new ResponseEntity("Subtask " + subtask.getId() + "  already present",
								HttpStatus.PRECONDITION_FAILED);
				}

				element.getSubtaskList().add(subtask);

			}
		}

		InterlinkGame result = gameComponent.saveOrUpdateGame(game.get());
		return new ResponseEntity(result, HttpStatus.OK);
	}

	/**
	 * Get a subtask
	 * 
	 * @param gameId
	 * @param taskId
	 * @param sutaskId
	 * @return Message
	 */
	@GetMapping(value = "/game/{gameId}/task/{taskId}/subtask/{subtaskId}")
	public Optional<InterlinkTask> getSubtask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId, @PathVariable(name = "subtaskId") String subtaskId) {

		Optional<InterlinkTask> subtask = Optional.empty();
		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (!game.isPresent()) {
			return subtask;
		}

		if (!game.get().isActive()) {
			return subtask;
		}

		for (InterlinkTask element : game.get().getTaskList()) {
			if (element.getId().equals(taskId)) {
				for (InterlinkTask subElement : element.getSubtaskList()) {
					if (subElement.getId().equals(subtaskId))
						subtask = Optional.of(subElement);
				}

			}
		}

		return subtask;
	}

	/**
	 * Claim a subtask
	 * 
	 * @param gameId
	 * @param taskId
	 * @param subtaskId
	 * @return Message
	 */
	@PutMapping(value = "/game/{gameId}/task/{taskId}/subtask/{subtaskId}/claim")
	public ResponseEntity<?> claimSubtask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId, @PathVariable(name = "subtaskId") String subtaskId,
			@RequestBody InterlinkPlayer player) {

		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (!game.isPresent()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}

		if (!game.get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}

		InterlinkTask task = game.get().getTaskList().stream().filter(t -> taskId.equals(t.getId())).findAny()
				.orElse(null);

		if (task != null) {
			InterlinkTask subTask = task.getSubtaskList().stream().filter(st -> subtaskId.equals(st.getId())).findAny()
					.orElse(null);
			if (subTask != null) {
				
				if (subTask.isCompleted()) {
					return new ResponseEntity("SubTask is already completed ", HttpStatus.PRECONDITION_FAILED);
				}
				
				InterlinkPlayer isPresent = subTask.getPlayers().stream().filter(p -> player.getId().equals(p.getId()))
						.findAny().orElse(null);
				if (isPresent == null) {
					subTask.addPlayer(player);
					InterlinkGame result = gameComponent.saveOrUpdateGame(game.get());
					return new ResponseEntity(result, HttpStatus.OK);
				} else {
					return new ResponseEntity("Player " + player.getId() + " already present inside task " + taskId,
							HttpStatus.PRECONDITION_FAILED);
				}
			}
		}

		return new ResponseEntity("Player " + player.getId() + " has non been added to subtask " + subtaskId,
				HttpStatus.PRECONDITION_FAILED);

	}
	
	/**
	 * Claim a subtask
	 * 
	 * @param gameId
	 * @param taskId
	 * @param subtaskId
	 * @return Message
	 */
	@DeleteMapping(value = "/game/{gameId}/task/{taskId}/subtask/{subtaskId}/removePlayer")
	public ResponseEntity<?> removeFromSubtask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId, @PathVariable(name = "subtaskId") String subtaskId,
			@RequestBody InterlinkPlayer player) {

		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (!game.isPresent()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}

		if (!game.get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}

		InterlinkTask task = game.get().getTaskList().stream().filter(t -> taskId.equals(t.getId())).findAny()
				.orElse(null);

		if (task != null) {
			InterlinkTask subTask = task.getSubtaskList().stream().filter(st -> subtaskId.equals(st.getId())).findAny()
					.orElse(null);
			if (subTask != null) {
				
				if (subTask.isCompleted()) {
					return new ResponseEntity("SubTask already completed ", HttpStatus.PRECONDITION_FAILED);
				}
				
				InterlinkPlayer isPresent = subTask.getPlayers().stream().filter(p -> player.getId().equals(p.getId()))
						.findAny().orElse(null);
				if (isPresent != null) {
					subTask.removePlayer(player);
					InterlinkGame result = gameComponent.saveOrUpdateGame(game.get());
					// remove from player state
					this.gamificationComponent.deletePlayerState(game.get().getProcessId(), game.get().getName(), player.getId());
					return new ResponseEntity(result, HttpStatus.OK);
				} else {
					return new ResponseEntity("Player " + player.getId() + " not present inside subtask " + subtaskId,
							HttpStatus.PRECONDITION_FAILED);
				}
			}
		}

		return new ResponseEntity("Player " + player.getId() + " has non been removed to subtask " + subtaskId,
				HttpStatus.PRECONDITION_FAILED);

	}

	/**
	 * Complete a subtask
	 * 
	 * @param gameId
	 * @param taskId
	 * @param subtaskId
	 * @return Message
	 */
	@PutMapping(value = "/game/{gameId}/task/{taskId}/subtask/{subtaskId}/complete")
	public ResponseEntity<?> completeSubtask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId, @PathVariable(name = "subtaskId") String subtaskId) {
		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);
		if (!game.isPresent()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}
		if (!game.get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}
		InterlinkTask savedTask = game.get().getTaskList().stream().filter(t -> taskId.equals(t.getId())).findAny()
				.orElse(null);
		if (savedTask != null) {
			InterlinkTask savedSubTask = savedTask.getSubtaskList().stream().filter(st -> subtaskId.equals(st.getId()))
					.findAny().orElse(null);
			if (savedSubTask != null) {
				if (savedSubTask.isCompleted()) {
					return new ResponseEntity("Subtask already completed", HttpStatus.PRECONDITION_FAILED);
				}
				// trigger player points assignment
				for (InterlinkPlayer player : savedSubTask.getPlayers()) {
					gamificationComponent.triggerAction(game.get().getProcessId(), game.get().getName(),
							"update_player_points", player, savedSubTask);
				}
				savedSubTask.setCompleted(true);
				InterlinkGame result = gameComponent.saveOrUpdateGame(game.get());
				return new ResponseEntity(result, HttpStatus.OK);
			} else {
				return new ResponseEntity("SubTask " + subtaskId + " is not present in task " + taskId,
						HttpStatus.PRECONDITION_FAILED);
			}

		} else {
			return new ResponseEntity("Task " + taskId + " is not present in game " + gameId,
					HttpStatus.PRECONDITION_FAILED);
		}
	}
	
	/**
	 * Revert a subtask with points
	 * 
	 * @param gameId
	 * @param taskId
	 * @param subtaskId
	 * @return Message
	 */
	@PutMapping(value = "/game/{gameId}/task/{taskId}/subtask/{subtaskId}/revert")
	public ResponseEntity<?> revertSubtask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId, @PathVariable(name = "subtaskId") String subtaskId) {
		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);
		if (!game.isPresent()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}
		if (!game.get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}
		InterlinkTask savedTask = game.get().getTaskList().stream().filter(t -> taskId.equals(t.getId())).findAny()
				.orElse(null);
		if (savedTask != null) {
			InterlinkTask savedSubTask = savedTask.getSubtaskList().stream().filter(st -> subtaskId.equals(st.getId()))
					.findAny().orElse(null);
			if (savedSubTask != null) {
				if (!savedSubTask.isCompleted()) {
					return new ResponseEntity("Subtask " + subtaskId + " must be completed",
							HttpStatus.PRECONDITION_FAILED);
				}
				// trigger player points assignement
				for (InterlinkPlayer player : savedSubTask.getPlayers()) {
					gamificationComponent.triggerAction(game.get().getProcessId(), game.get().getName(),
							"revert_player_points", player, savedSubTask);
				}
				savedSubTask.setCompleted(false);
				InterlinkGame result = gameComponent.saveOrUpdateGame(game.get());
				return new ResponseEntity(result, HttpStatus.OK);
			} else {
				return new ResponseEntity("SubTask " + subtaskId + " is not present in task " + taskId,
						HttpStatus.PRECONDITION_FAILED);
			}

		} else {
			return new ResponseEntity("Task " + taskId + " is not present in game " + gameId,
					HttpStatus.PRECONDITION_FAILED);
		}
	}

	/**
	 * Update a subtask
	 * 
	 * @param gameId
	 * @param taskId
	 * @param subtask
	 * @return Message
	 */
	@PutMapping(value = "/game/{gameId}/task/{taskId}/subtask")
	public ResponseEntity<?> updateSubtask(@PathVariable(name = "gameId") String gameId,
			@PathVariable(name = "taskId") String taskId, @RequestBody InterlinkTask subtask) {

		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (!game.isPresent()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}

		if (!game.get().isActive()) {
			return new ResponseEntity("Game is suspended", HttpStatus.PRECONDITION_FAILED);
		}

		InterlinkTask savedTask = game.get().getTaskList().stream().filter(t -> taskId.equals(t.getId())).findAny()
				.orElse(null);

		if (savedTask != null) {
			InterlinkTask savedSubTask = savedTask.getSubtaskList().stream()
					.filter(st -> subtask.getId().equals(st.getId())).findAny().orElse(null);

			if (savedSubTask != null) {
				
				if (savedSubTask.isCompleted()) {
					return new ResponseEntity("SubTask is already completed ", HttpStatus.PRECONDITION_FAILED);
				}
				
				savedSubTask.setDevelopment(subtask.getDevelopment());
				savedSubTask.setManagement(subtask.getManagement());
				savedSubTask.setExploitation(subtask.getExploitation());
				savedSubTask.setCompleted(subtask.isCompleted());
				for (InterlinkPlayer updated : subtask.getPlayers()) {
					InterlinkPlayer savedPlayer = savedSubTask.getPlayers().stream()
							.filter(p -> p.getId().equals(updated.getId())).findAny().orElse(null);
					if (savedPlayer != null) {
						savedPlayer.setName(updated.getName());
						savedPlayer.setDevelopment(updated.getDevelopment());
						savedPlayer.setExploitation(updated.getExploitation());
						savedPlayer.setManagement(updated.getManagement());
					} else {
						savedSubTask.getPlayers().add(updated);
					}
				}
				InterlinkGame result = gameComponent.saveOrUpdateGame(game.get());
				return new ResponseEntity(result, HttpStatus.OK);
			} else {
				return new ResponseEntity("SubTask " + subtask.getId() + " not present inside game",
						HttpStatus.PRECONDITION_FAILED);
			}
		} else {
			return new ResponseEntity("Task " + taskId + " not present inside game", HttpStatus.PRECONDITION_FAILED);
		}
	}

	/**
	 * Suspend game
	 */
	@PutMapping(value = "/game/{gameId}/suspend")
	public ResponseEntity<?> suspendGame(@PathVariable String gameId) {

		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (game.isEmpty()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}

		InterlinkGame gameToUpdate = game.get();

		gameToUpdate.setActive(false);

		this.gameComponent.saveOrUpdateGame(gameToUpdate);

		return new ResponseEntity("Game has been suspened successfully", HttpStatus.OK);
	}

	/**
	 * Resume game
	 */
	@PutMapping(value = "/game/{gameId}/resume")
	public ResponseEntity<?> resumeGame(@PathVariable String gameId) {

		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (game.isEmpty()) {
			return new ResponseEntity("Game is not present", HttpStatus.PRECONDITION_FAILED);
		}

		InterlinkGame gameToUpdate = game.get();

		gameToUpdate.setActive(true);

		this.gameComponent.saveOrUpdateGame(gameToUpdate);

		return new ResponseEntity("Game has been resumed successfully", HttpStatus.OK);
	}

	/**
	 * Get the array of player profile for a game
	 * 
	 * @param gameId
	 */

	@GetMapping(value = "/game/{gameId}/player")
	public List<String> getPlayers(@PathVariable String gameId) {
		List<String> players = new ArrayList<String>();

		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (game.isEmpty()) {
			return players;
		}

		players = this.gamificationComponent.getPlayers(game.get().getProcessId(), game.get().getName());
		return players;

	}

	/**
	 * Get player profile
	 */

	@GetMapping(value = "/game/{gameId}/player/{playerId}")
	public PlayerStateDTO getPlayerState(@PathVariable String gameId, @PathVariable String playerId) {
		Optional<PlayerState> playerState = Optional.empty();

		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (game.isEmpty()) {
			return null;
		}

		playerState = Optional.of(
				this.gamificationComponent.getPlayerState(game.get().getProcessId(), game.get().getName(), playerId));
		return ControllerUtils.convertPlayerState(playerState.get());

	}

	/**
	 * Get game statistic
	 * 
	 * @param gameId
	 */

	@GetMapping(value = "/game/{gameId}/stats/{pointConcept}")
	public List<GameStatistics> getGameStatistic(@PathVariable String gameId, @PathVariable String pointConcept) {
		List<GameStatistics> stats = new ArrayList<GameStatistics>();

		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (game.isEmpty()) {
			return stats;
		}

		stats = this.gamificationComponent.getGameStats(game.get().getProcessId(), game.get().getName(), pointConcept);
		return stats;

	}

	@GetMapping(value = "/game/{gameId}/player/search")
	public Page<PlayerScore> searchByQuery(@PathVariable @ApiParam(name = "gameId") String gameId,
			@RequestParam @ApiParam(name = "period", value = "global", allowableValues = "currentWeek, previousWeek, global") String period,
			@RequestParam @ApiParam(name = "activityType", value = "management", allowableValues = "development, management, exploitation") String activityType,
			Pageable pageable) {
		gameId = ControllerUtils.decodePathVariable(gameId);
		Optional<InterlinkGame> game = gameComponent.findById(gameId);

		if (game.isEmpty()) {
			return null;
		}

		gameId = ControllerUtils.getGameId(game.get().getProcessId(), game.get().getName());
		return interlinkRepo.search(gameId, activityType, period, pageable);

	}

}
