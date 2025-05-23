package meety.services;

import meety.dtos.GroupDto;
import meety.models.Group;
import meety.models.GroupMember;
import meety.models.User;
import meety.models.enums.Role;
import meety.repositories.GroupMemberRepository;
import meety.repositories.GroupRepository;
import meety.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GroupService {
    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Group> getPublicGroups() {
        List<Group> allGroups = groupRepository.findAll();
        return allGroups.stream().filter(Group::getIsPublic).toList();
    }

    public Optional<Group> getGroupById(Long id) {
        return groupRepository.findById(id);
    }

    public Group createGroup(GroupDto dto, User creator) {
        Group group = new Group(dto);
        group.setCreatedBy(creator);

        group = groupRepository.save(group);
        groupMemberRepository.save(new GroupMember(group, creator, Role.ADMIN));
        return group;
    }

    public Group updateGroup(Long id, GroupDto updatedGroup) {
        return groupRepository.findById(id).map(existing -> {
            existing.setName(updatedGroup.getName());
            existing.setDescription(updatedGroup.getDescription());
            existing.setIsPublic(updatedGroup.getIsPublic());
            return groupRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Group not found with id: " + id));
    }

    public void deleteGroup(Long id) {
        groupRepository.deleteById(id);
    }

    public Group joinGroup(Long groupId, User currentUser) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        boolean alreadyMember = groupMemberRepository.findByGroupAndUser(group, currentUser).isPresent();
        if (alreadyMember) {
            throw new RuntimeException("User is already a member");
        }

        GroupMember newMember = new GroupMember(group, currentUser, Role.USER);
        groupMemberRepository.save(newMember);

        return group;
    }

    public Group leaveGroup(Long groupId, User currentUser) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        GroupMember member = groupMemberRepository.findByGroupAndUser(group, currentUser)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        groupMemberRepository.delete(member);
        return group;
    }

    public List<User> getMembers(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        List<GroupMember> groupMembers = groupMemberRepository.findAllByGroup(group);
        return groupMembers.stream()
                .map(GroupMember::getUser)
                .toList();
    }


    public Group removeMember(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        groupMemberRepository.deleteByGroupAndUser(group, user);
        return group;
    }


}
